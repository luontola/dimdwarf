// Copyright © 2008-2009, Esko Luontola. All Rights Reserved.
// This software is released under the MIT License.
// The license may be viewed at http://dimdwarf.sourceforge.net/LICENSE

package net.orfjackal.dimdwarf.gc.entities;

import com.google.inject.Inject;
import net.orfjackal.dimdwarf.api.internal.ObjectIdMigration;
import net.orfjackal.dimdwarf.db.Blob;
import net.orfjackal.dimdwarf.entities.*;
import net.orfjackal.dimdwarf.entities.dao.EntityDao;
import net.orfjackal.dimdwarf.gc.MutatorListener;
import net.orfjackal.dimdwarf.scopes.TaskScoped;
import net.orfjackal.dimdwarf.serial.*;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.*;

/**
 * @author Esko Luontola
 * @since 1.9.2008
 */
@TaskScoped
@NotThreadSafe
public class GcAwareEntityRepository implements EntityRepository {

    private final EntityDao entities;
    private final ObjectSerializer serializer;
    private final MutatorListener<ObjectIdMigration> listener;

    private final Map<ObjectIdMigration, Set<ObjectIdMigration>> referencesOnRead = new HashMap<ObjectIdMigration, Set<ObjectIdMigration>>();

    @Inject
    public GcAwareEntityRepository(EntityDao entities,
                                   ObjectSerializer serializer,
                                   MutatorListener<ObjectIdMigration> listener) {
        this.entities = entities;
        this.serializer = serializer;
        this.listener = listener;
    }

    public boolean exists(ObjectIdMigration id) {
        return entities.exists(id);
    }

    public Object read(ObjectIdMigration id) {
        DeserializationResult oldData = readFromDatabase(id);
        cacheReferencesOnRead(id, oldData);
        return oldData.getDeserializedObject();
    }

    private DeserializationResult readFromDatabase(ObjectIdMigration id) {
        Blob bytes = entities.read(id);
        if (bytes.equals(Blob.EMPTY_BLOB)) {
            throw new EntityNotFoundException("id=" + id);
        }
        return serializer.deserialize(bytes);
    }

    private void cacheReferencesOnRead(ObjectIdMigration id, DeserializationResult oldData) {
        Set<ObjectIdMigration> oldReferences = getReferencedEntities(oldData);
        referencesOnRead.put(id, oldReferences);
    }

    private static Set<ObjectIdMigration> getReferencedEntities(ResultWithMetadata result) {
        List<ObjectIdMigration> possibleDuplicates = result.getMetadata(EntityReferenceListener.class);
        return new HashSet<ObjectIdMigration>(possibleDuplicates);
    }

    public void update(ObjectIdMigration id, Object entity) {
        SerializationResult newData = serializer.serialize(entity);
        if (hasBeenModified(id, newData)) {
            entities.update(id, newData.getSerializedBytes());
            fireEntityUpdated(id, newData);
        }
    }

    private boolean hasBeenModified(ObjectIdMigration id, SerializationResult newData) {
        Blob oldBytes = entities.read(id);
        Blob newBytes = newData.getSerializedBytes();
        return !oldBytes.equals(newBytes);
    }

    private void fireEntityUpdated(ObjectIdMigration id, SerializationResult newData) {
        Set<ObjectIdMigration> newReferences = getReferencedEntities(newData);
        Set<ObjectIdMigration> oldReferences = referencesOnRead.remove(id);
        if (oldReferences == null) {
            oldReferences = Collections.emptySet();
            fireEntityCreated(id);
        }
        fireReferencesRemoved(id, newReferences, oldReferences);
        fireReferencesCreated(id, newReferences, oldReferences);
    }

    private void fireEntityCreated(ObjectIdMigration id) {
        listener.onNodeCreated(id);
    }

    private void fireReferencesRemoved(ObjectIdMigration id, Set<ObjectIdMigration> newReferences, Set<ObjectIdMigration> oldReferences) {
        for (ObjectIdMigration targetId : oldReferences) {
            if (!newReferences.contains(targetId)) {
                listener.onReferenceRemoved(id, targetId);
            }
        }
    }

    private void fireReferencesCreated(ObjectIdMigration id, Set<ObjectIdMigration> newReferences, Set<ObjectIdMigration> oldReferences) {
        for (ObjectIdMigration targetId : newReferences) {
            if (!oldReferences.contains(targetId)) {
                listener.onReferenceCreated(id, targetId);
            }
        }
    }

    public void delete(ObjectIdMigration id) {
        DeserializationResult oldData = readFromDatabase(id);
        entities.delete(id);
        fireEntityDeleted(id, oldData);
    }

    private void fireEntityDeleted(ObjectIdMigration id, DeserializationResult oldData) {
        Set<ObjectIdMigration> newReferences = Collections.emptySet();
        Set<ObjectIdMigration> oldReferences = getReferencedEntities(oldData);
        fireReferencesRemoved(id, newReferences, oldReferences);
    }

    public ObjectIdMigration firstKey() {
        return entities.firstKey();
    }

    public ObjectIdMigration nextKeyAfter(ObjectIdMigration currentKey) {
        return entities.nextKeyAfter(currentKey);
    }
}
