// Copyright © 2008-2013 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://dimdwarf.sourceforge.net/LICENSE

package net.orfjackal.dimdwarf;

import fi.jumi.launcher.JumiBootstrap;
import org.junit.Test;

public class JumiSuite {

    @Test
    public void all_tests() throws Exception {
        JumiBootstrap bootstrap = new JumiBootstrap();
        bootstrap.suite
                .addJvmOptions("-ea")
                .setIncludedTestsPattern("glob:net/orfjackal/dimdwarf/**{Spec,Test}.class");
        bootstrap
                //.enableDebugMode()
                //.setPassingTestsVisible(true)
                .runSuite();
    }
}
