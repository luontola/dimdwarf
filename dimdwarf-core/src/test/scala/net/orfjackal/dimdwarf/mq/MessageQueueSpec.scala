// Copyright © 2008-2012 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://dimdwarf.sourceforge.net/LICENSE

package net.orfjackal.dimdwarf.mq

import org.specs.SpecsMatchers
import org.specsy.scala.ScalaSpecsy

class MessageQueueSpec extends ScalaSpecsy with SpecsMatchers {
  val queue = new MessageQueue[String]("Unnamed")

  "Messages are read from the queue in FIFO order" >> {
    queue.send("sent first")
    queue.send("sent second")

    queue.take() must_== "sent first"
    queue.take() must_== "sent second"
  }

  "When the queue is empty, take() will wait until there is a message" >> {
    val asynchronousSend = new Thread(new Runnable {
      def run() {
        queue.send("async message")
      }
    })

    asynchronousSend.start()
    queue.take() must_== "async message"
  }

  "When the queue is empty, poll() will return immediately" >> {
    queue.send("message")
    queue.poll() must_== "message"

    queue.poll() must beNull
  }
}
