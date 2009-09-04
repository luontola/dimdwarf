// Copyright © 2008-2009, Esko Luontola. All Rights Reserved.
// This software is released under the MIT License.
// The license may be viewed at http://dimdwarf.sourceforge.net/LICENSE

package net.orfjackal.dimdwarf.entities;

import com.google.inject.*;
import net.orfjackal.dimdwarf.api.internal.ObjectIdMigration;

import javax.annotation.concurrent.ThreadSafe;

/**
 * @author Esko Luontola
 * @since 4.9.2008
 */
@Singleton
@ThreadSafe
public class EntityIdFactoryImpl implements EntityIdFactory {

    // using java.util.concurrent.atomic.AtomicLong would also be an option

    private ObjectIdMigration nextId;

    @Inject
    public EntityIdFactoryImpl(@MaxEntityId ObjectIdMigration largestUsedId) {
        nextId = largestUsedId.next();
    }

    public synchronized ObjectIdMigration newId() {
        ObjectIdMigration currentId = nextId;
        nextId = nextId.next();
        return currentId;
    }
}
