package net.orfjackal.dimdwarf.net

import org.junit.runner.RunWith
import org.hamcrest.Matchers._
import org.hamcrest.MatcherAssert.assertThat
import net.orfjackal.specsy._
import net.orfjackal.dimdwarf.mq.MessageQueue
import net.orfjackal.dimdwarf.auth._
import net.orfjackal.dimdwarf.actors._
import net.orfjackal.dimdwarf.net.sgs._
import net.orfjackal.dimdwarf.db.Blob

@RunWith(classOf[Specsy])
class SessionMessagesSpec extends Spec {
  val queues = new DeterministicMessageQueues

  // TODO: remove duplication between the setups of this test and LoginLogoutSpec 
  val toAuthenticator = new MessageQueue[AuthenticatorMessage]("toAuthenticator")
  val authenticatorActor = new AuthenticatorActor(queues.toHub, new DummyCredentialsChecker)
  queues.addActor(authenticatorActor, toAuthenticator)
  val authenticatorCtrl = new AuthenticatorController(toAuthenticator)
  queues.addController(authenticatorCtrl)

  val toNetwork = new MessageQueue[NetworkMessage]("toNetwork")
  val networkActor = new DummyNetworkActor()
  queues.addActor(networkActor, toNetwork)
  val networkCtrl = new NetworkController(toNetwork, authenticatorCtrl)
  queues.addController(networkCtrl)

  // given client has connected
  clientSends(LoginRequest("John Doe", "secret"))
  assertMessageSent(toNetwork, SendToClient(LoginSuccess(), DummySessionHandle()))

  "When client sends a session message" >> {
    val message = Blob.fromBytes("hello".getBytes)
    clientSends(SessionMessage(message))
    // TODO: handle the session message in a worker thread
  }

  // TODO: when a task commits, send session messages to clients
  // TODO: when a task rolls back, no session messages are sent

  private def assertMessageSent(queue: MessageQueue[NetworkMessage], expected: Any) {
    assertThat(queues.seenIn(queue).head, is(expected))
  }

  private def clientSends(message: ClientMessage) {
    queues.toHub.send(ReceivedFromClient(message, DummySessionHandle()))
    queues.processMessagesUntilIdle()
  }

  class DummyNetworkActor extends Actor[NetworkMessage] {
    def start() {}

    def process(message: NetworkMessage) {}
  }

  case class DummySessionHandle() extends SessionHandle

  class DummyCredentialsChecker extends CredentialsChecker[Credentials] {
    def isValid(credentials: Credentials) = true
  }
}
