// Copyright © 2008-2009, Esko Luontola. All Rights Reserved.
// This software is released under the MIT License.
// The license may be viewed at http://dimdwarf.sourceforge.net/LICENSE

package net.orfjackal.dimdwarf.gc.entities;

import com.google.inject.Inject;
import net.orfjackal.dimdwarf.api.internal.ObjectIdMigration;
import net.orfjackal.dimdwarf.db.*;
import net.orfjackal.dimdwarf.entities.dao.*;
import net.orfjackal.dimdwarf.gc.Graph;
import net.orfjackal.dimdwarf.serial.*;
import net.orfjackal.dimdwarf.util.SerializableIterable;

import java.io.Serializable;
import java.util.*;

/**
 * @author Esko Luontola
 * @since 30.11.2008
 */
public class EntityGraph implements Graph<ObjectIdMigration> {

    private final EntityDao entities;
    private final BindingDao bindings;
    private final ObjectSerializer serializer;

    @Inject
    public EntityGraph(EntityDao entities, BindingDao bindings, ObjectSerializer serializer) {
        this.entities = entities;
        this.bindings = bindings;
        this.serializer = serializer;
    }

    public Iterable<ObjectIdMigration> getAllNodes() {
        return new Iterable<ObjectIdMigration>() {
            public Iterator<ObjectIdMigration> iterator() {
                return new AllNodesIterator(entities);
            }
        };
    }

    public Iterable<ObjectIdMigration> getRootNodes() {
        return new Iterable<ObjectIdMigration>() {
            public Iterator<ObjectIdMigration> iterator() {
                return new RootNodesIterator(bindings);
            }
        };
    }

    public Iterable<ObjectIdMigration> getConnectedNodesOf(ObjectIdMigration node) {
        Blob entity = entities.read(node);
        List<ObjectIdMigration> ids = getReferencedEntityIds(entity);
        return new SerializableIterable<ObjectIdMigration>(ids);
    }

    private List<ObjectIdMigration> getReferencedEntityIds(Blob entity) {
        DeserializationResult result = serializer.deserialize(entity);
        return result.getMetadata(EntityReferenceListener.class);
    }

    public void removeNode(ObjectIdMigration node) {
        entities.delete(node);
    }

    public byte[] getMetadata(ObjectIdMigration node, String metaKey) {
        if (entities.exists(node)) {
            return entities.readMetadata(node, metaKey).getByteArray();
        } else {
            return new byte[0];
        }
    }

    public void setMetadata(ObjectIdMigration node, String metaKey, byte[] metaValue) {
        entities.updateMetadata(node, metaKey, Blob.fromBytes(metaValue));
    }


    private static class AllNodesIterator implements Iterator<ObjectIdMigration>, Serializable {
        private static final long serialVersionUID = 1L;

        private final DatabaseKeyIterator<ObjectIdMigration> iterator = new DatabaseKeyIterator<ObjectIdMigration>();

        public AllNodesIterator(EntityDao entities) {
            setEntityDao(entities);
        }

        @Inject
        public void setEntityDao(EntityDao entities) {
            iterator.setTable(entities);
        }

        public boolean hasNext() {
            return iterator.hasNext();
        }

        public ObjectIdMigration next() {
            return iterator.next();
        }

        public void remove() {
            iterator.remove();
        }
    }

    private static class RootNodesIterator implements Iterator<ObjectIdMigration>, Serializable {
        private static final long serialVersionUID = 1L;

        private final DatabaseKeyIterator<String> iterator = new DatabaseKeyIterator<String>();
        private transient BindingDao bindings;

        public RootNodesIterator(BindingDao bindings) {
            setBindingDao(bindings);
        }

        @Inject
        public void setBindingDao(BindingDao bindings) {
            this.bindings = bindings;
            iterator.setTable(bindings);
        }

        public boolean hasNext() {
            return iterator.hasNext();
        }

        public ObjectIdMigration next() {
            String binding = iterator.next();
            return bindings.read(binding);
        }

        public void remove() {
            iterator.remove();
        }
    }
}
