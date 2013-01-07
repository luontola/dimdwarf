// Copyright © 2008-2013 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://dimdwarf.sourceforge.net/LICENSE

package net.orfjackal.dimdwarf

import org.junit.Test
import fi.jumi.launcher.JumiBootstrap

class JumiBootstrapSpec {

  @Test
  def runJumiTests() {
    // TODO: when upgrading to use patterns, make sure to exclude this test (it would produce infinite recursion)
    new JumiBootstrap()
            .setPassingTestsVisible(false)
            .runTestClasses(
      classOf[actors.ActorStarterSpec],
      classOf[actors.InstallingActorsSpec],
      classOf[auth.AuthenticatorSpec],
      classOf[controller.ControllerHubSpec],
      classOf[domain.ClockSpec],
      classOf[domain.SessionIdFactorySpec],
      classOf[domain.SimpleTimestampSpec],
      classOf[mq.MessageQueueSpec],
      classOf[net.ClientSessionsSpec],
      classOf[net.LoginLogoutSpec],
      classOf[net.NetworkActorSpec],
      classOf[net.SessionMessagesSpec],
      classOf[net.sgs.SimpleSgsProtocolSpec],
      classOf[server.ApplicationLoadingSpec],
      classOf[server.CommandLineArgumentsSpec],
      classOf[server.ModuleConfigurationSpec],
      classOf[tasks2.TaskExecutorSpec]
    )
  }
}
