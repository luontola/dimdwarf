package net.orfjackal.dimdwarf.tasks2

import org.hamcrest.Matchers._
import org.hamcrest.MatcherAssert.assertThat
import org.junit.runner.RunWith
import net.orfjackal.specsy._
import net.orfjackal.dimdwarf.mq.MessageQueue
import net.orfjackal.dimdwarf.net._
import net.orfjackal.dimdwarf.db.Blob
import net.orfjackal.dimdwarf.net.sgs.SessionMessage

@RunWith(classOf[Specsy])
class TaskExecutorSpec extends Spec {
  val toNetwork = new MessageQueue[NetworkMessage]("toNetwork")
  val taskExecutor = new TaskExecutor(toNetwork)

  val session = DummySessionHandle()
  val message = Blob.fromBytes("hello".getBytes)

  "TEMPORARY INTEGRATION TEST" >> {
    // TODO: deepen the design, split this test into smaller pieces
    taskExecutor.processSessionMessage(session, message)

    assertThat(toNetwork.take(), is(SendToClient(SessionMessage(message), session): Any))
  }

  case class DummySessionHandle() extends SessionHandle
}
