// Copyright © 2008-2009, Esko Luontola. All Rights Reserved.
// This software is released under the MIT License.
// The license may be viewed at http://dimdwarf.sourceforge.net/LICENSE

package net.orfjackal.dimdwarf.api.internal;

import java.math.BigInteger;

/**
 * @author Esko Luontola
 * @since 13.8.2009
 */
public class ObjectIdMigration {

    private final BigInteger bi;

    public ObjectIdMigration(int signum, byte[] magnitude) {
        bi = new BigInteger(signum, magnitude);
    }

    public ObjectIdMigration(byte[] val) {
        bi = new BigInteger(val);
    }

    public ObjectIdMigration(long val) {
        bi = BigInteger.valueOf(val);
    }

    public static ObjectIdMigration valueOf(long val) {
        return new ObjectIdMigration(val);
    }

    public byte[] toByteArray() {
        return bi.toByteArray();
    }

    public ObjectIdMigration add(ObjectIdMigration val) {
        return new ObjectIdMigration(val.bi.longValue() + 1);
    }

    public int hashCode() {
        return bi.hashCode();
    }

    public boolean equals(Object obj) {
        return bi.equals(obj);
    }

    public String toString() {
        return bi.toString();
    }
}
