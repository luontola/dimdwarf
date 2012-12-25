// Copyright © 2008-2012 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://dimdwarf.sourceforge.net/LICENSE

package net.orfjackal.dimdwarf.server

import com.google.inject._
import net.orfjackal.dimdwarf.auth._
import org.specsy.scala.ScalaSpecsy

class ModuleConfigurationSpec extends ScalaSpecsy {
  val port = 1000
  val appModule = new AbstractModule {
    def configure() {
      bind(classOf[CredentialsChecker[_]]).toInstance(new CredentialsChecker[Credentials] {
        def isValid(credentials: Credentials) = false
      })
    }
  }

  "DI configuration has no errors" >> {
    val modules = Main.configureServerModules(port, appModule)

    // throws CreationException if there is an error
    Guice.createInjector(modules)
  }
}
