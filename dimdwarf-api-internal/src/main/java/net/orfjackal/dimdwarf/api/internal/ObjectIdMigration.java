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

    public static final ObjectIdMigration ZERO = new ObjectIdMigration(0);
    public static final ObjectIdMigration ONE = new ObjectIdMigration(1);
    public static final ObjectIdMigration TEN = new ObjectIdMigration(10);

    public final BigInteger bigId;

    public ObjectIdMigration(int signum, byte[] magnitude) {
        bigId = new BigInteger(signum, magnitude);
    }

    public ObjectIdMigration(byte[] val) {
        bigId = new BigInteger(val);
    }

    public ObjectIdMigration(long val) {
        bigId = BigInteger.valueOf(val);
    }

    public static ObjectIdMigration valueOf(long val) {
        return new ObjectIdMigration(val);
    }

    public byte[] toByteArray() {
        return bigId.toByteArray();
    }

    public ObjectIdMigration add(ObjectIdMigration that) {
        return new ObjectIdMigration(this.bigId.longValue() + that.bigId.longValue());
    }

    public int hashCode() {
        return bigId.hashCode();
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof ObjectIdMigration)) {
            return false;
        }
        ObjectIdMigration that = (ObjectIdMigration) obj;
        return bigId.equals(that.bigId);
    }

    public String toString() {
        return bigId.toString();
    }

    public int signum() {
        return bigId.signum();
    }
}
