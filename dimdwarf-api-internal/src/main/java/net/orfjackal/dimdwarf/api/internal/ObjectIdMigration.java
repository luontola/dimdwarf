// Copyright © 2008-2009, Esko Luontola. All Rights Reserved.
// This software is released under the MIT License.
// The license may be viewed at http://dimdwarf.sourceforge.net/LICENSE

package net.orfjackal.dimdwarf.api.internal;

import java.io.Serializable;
import java.math.BigInteger;

/**
 * @author Esko Luontola
 * @since 13.8.2009
 */
public class ObjectIdMigration implements Serializable {

    public final ObjectId objId;

    public ObjectIdMigration(long val) {
        objId = new ObjectId(val);
    }

    private ObjectIdMigration(ObjectId objId) {
        this.objId = objId;
    }

    public ObjectIdMigration next() {
        return new ObjectIdMigration(this.objId.next());
    }

    public int hashCode() {
        return objId.hashCode();
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof ObjectIdMigration)) {
            return false;
        }
        ObjectIdMigration that = (ObjectIdMigration) obj;
        return objId.equals(that.objId);
    }

    public String toString() {
        return objId.toString();
    }

    public BigInteger toBigInteger() {
        return objId.toBigInteger();
    }
}
