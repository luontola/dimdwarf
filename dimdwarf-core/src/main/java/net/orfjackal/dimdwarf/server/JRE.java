// Copyright © 2008-2010 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://dimdwarf.sourceforge.net/LICENSE

package net.orfjackal.dimdwarf.server;

import java.net.URLClassLoader;

public class JRE {

    public static boolean isJava7() {
        try {
            URLClassLoader.class.getMethod("close");
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    /**
     * URLClassLoader locks any JARs from which it loads resources, which
     * will prevent removing the JAR file until the ClassLoader is garbage collected.
     * The {@link java.net.URLClassLoader#close()} method was added in Java 7 to
     * solve this problem.
     * <p/>
     * Related issues and some workarounds:
     * http://bugs.sun.com/view_bug.do?bug_id=4950148
     * http://bugs.sun.com/view_bug.do?bug_id=4167874
     * http://download.oracle.com/javase/7/docs/technotes/guides/net/ClassLoader.html
     *
     * @see java.net.URLClassLoader#close()
     */
    public static void closeClassLoader(URLClassLoader cl) {
        try {
            URLClassLoader.class.getMethod("close").invoke(cl);
        } catch (Exception e) {
            throw new RuntimeException("Cannot invoke URLClassLoader.close()", e);
        }
    }
}
