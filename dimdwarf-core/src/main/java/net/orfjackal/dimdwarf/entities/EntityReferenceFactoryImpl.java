// Copyright © 2008-2013 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://dimdwarf.sourceforge.net/LICENSE

package net.orfjackal.dimdwarf.entities;

import net.orfjackal.dimdwarf.api.EntityId;
import net.orfjackal.dimdwarf.api.internal.*;

import javax.annotation.concurrent.NotThreadSafe;
import javax.inject.Inject;

@NotThreadSafe
public class EntityReferenceFactoryImpl implements EntityReferenceFactory {

    private final AllEntities entities;

    @Inject
    public EntityReferenceFactoryImpl(AllEntities entities) {
        this.entities = entities;
    }

    public <T> EntityReference<T> createReference(T entity) {
        EntityId id = entities.getEntityId((EntityObject) entity);
        return new EntityReferenceImpl<>(id, entity);
    }
}
