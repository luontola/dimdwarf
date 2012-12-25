// Copyright © 2008-2012 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://dimdwarf.sourceforge.net/LICENSE

package net.orfjackal.dimdwarf.controller

import org.scalatest.matchers.ShouldMatchers
import org.mockito.Mockito._
import org.mockito.Matchers._
import org.specsy.scala.ScalaSpecsy

class ControllerHubSpec extends ScalaSpecsy with ShouldMatchers {
  val hub = new ControllerHub
  val controller1 = mock(classOf[Controller], "controller1")
  val controller2 = mock(classOf[Controller], "controller2")

  hub.addController(controller1)
  hub.addController(controller2)

  "Delegates messages to all controllers" >> {
    hub.process("message")

    verify(controller1).process("message")
    verify(controller2).process("message")
  }

  "Controllers are invoked in the order they were registered" >> {
    hub.process("message")

    val order = inOrder(controller1, controller2)
    order.verify(controller1).process(anyObject)
    order.verify(controller2).process(anyObject)
  }
}
