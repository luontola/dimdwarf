// Copyright © 2008-2009, Esko Luontola. All Rights Reserved.
// This software is released under the MIT License.
// The license may be viewed at http://dimdwarf.sourceforge.net/LICENSE

package net.orfjackal.dimdwarf.entities;

import net.orfjackal.dimdwarf.api.internal.ObjectIdMigration;
import net.orfjackal.dimdwarf.db.DatabaseTable;

import javax.annotation.Nonnull;

/**
 * @author Esko Luontola
 * @since 31.8.2008
 */
public interface EntityRepository extends DatabaseTable<ObjectIdMigration, Object> {

    @Nonnull
    Object read(ObjectIdMigration id) throws EntityNotFoundException;

    void update(ObjectIdMigration id, Object entity);

    void delete(ObjectIdMigration id);
}
