// Copyright © 2008-2009, Esko Luontola. All Rights Reserved.
// This software is released under the MIT License.
// The license may be viewed at http://dimdwarf.sourceforge.net/LICENSE

package net.orfjackal.dimdwarf.entities;

import jdave.*;
import jdave.junit4.JDaveRunner;
import net.orfjackal.dimdwarf.api.internal.ObjectIdMigration;
import org.junit.runner.RunWith;

/**
 * @author Esko Luontola
 * @since 4.9.2008
 */
@RunWith(JDaveRunner.class)
@Group({"fast"})
public class EntityIdFactorySpec extends Specification<Object> {

    private static final ObjectIdMigration LARGEST_USED_ID = ObjectIdMigration.valueOf(42);

    private EntityIdFactoryImpl factory;

    public void create() throws Exception {
        factory = new EntityIdFactoryImpl(LARGEST_USED_ID);
    }


    public class AnEntityIdFactory {

        public void startsFromTheNextUnusedId() {
            ObjectIdMigration nextUnused = LARGEST_USED_ID.add(ObjectIdMigration.ONE);
            specify(factory.newId(), should.equal(nextUnused));
        }

        public void incrementsTheIdOnEveryCall() {
            ObjectIdMigration id1 = factory.newId();
            ObjectIdMigration id2 = factory.newId();
            specify(id2, should.equal(id1.add(ObjectIdMigration.ONE)));
        }
    }
}
