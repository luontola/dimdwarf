// Copyright © 2008-2013 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://dimdwarf.sourceforge.net/LICENSE

package net.orfjackal.dimdwarf

import org.junit.Test
import fi.jumi.launcher.JumiBootstrap

class JumiBootstrapSuite {

  @Test
  def runJumiTests() {
    new JumiBootstrap()
            //.enableDebugMode()
            .setPassingTestsVisible(false)
            .runTestsMatching("glob:net/orfjackal/dimdwarf/**Spec.class")
  }
}
