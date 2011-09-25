package net.orfjackal.dimdwarf.tasks2

import org.hamcrest.Matchers._
import org.hamcrest.MatcherAssert.assertThat
import org.junit.runner.RunWith
import net.orfjackal.specsy._
import net.orfjackal.dimdwarf.mq.MessageQueue
import net.orfjackal.dimdwarf.net._
import net.orfjackal.dimdwarf.db.Blob
import net.orfjackal.dimdwarf.domain._
import net.orfjackal.dimdwarf.actors._
import net.orfjackal.dimdwarf.auth._
import net.orfjackal.dimdwarf.net.sgs._

@RunWith(classOf[Specsy])
class TaskExecutorSpec extends Spec {
  val queues = new DeterministicMessageQueues
  val authenticator = new FakeAuthenticator
  val clock = new Clock(SimpleTimestamp(100L))
  val networkActor = new DummyNetworkActor()

  val toNetwork = new MessageQueue[NetworkMessage]("toNetwork")
  queues.addActor(networkActor, toNetwork)

  val taskExecutor = new TaskExecutor(queues.toHub)
  val networkCtrl = new NetworkController(toNetwork, authenticator, taskExecutor, clock)
  queues.addController(networkCtrl)




  val session = DummySessionHandle(50)
  val message = Blob.fromBytes("hello".getBytes)

  "TEMPORARY INTEGRATION TEST" >> {
    // TODO: deepen the design, split this test into smaller pieces
    queues.toHub.send(ReceivedFromClient(LoginRequest("username", "password"), session))
    queues.toHub.send(ReceivedFromClient(SessionMessage(message), session))
    queues.processMessagesUntilIdle()

    val expectedReply = SendToClient(SessionMessage(message), session)
    assertThat(queues.seenIn(toNetwork).last, is(expectedReply: Any))
  }


  class FakeAuthenticator extends Authenticator {
    def isUserAuthenticated(credentials: Credentials, onYes: => Unit, onNo: => Unit) {
      onYes
    }
  }

  class DummyNetworkActor extends Actor[NetworkMessage] {
    def start() {}

    def process(message: NetworkMessage) {}
  }

  case class DummySessionHandle(id: Int) extends SessionHandle
}
