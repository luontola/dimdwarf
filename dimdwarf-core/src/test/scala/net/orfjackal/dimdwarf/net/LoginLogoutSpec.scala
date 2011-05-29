package net.orfjackal.dimdwarf.net

import org.junit.runner.RunWith
import org.hamcrest.Matchers._
import org.hamcrest.MatcherAssert.assertThat
import net.orfjackal.specsy._
import net.orfjackal.dimdwarf.mq.MessageQueue
import net.orfjackal.dimdwarf.auth._
import net.orfjackal.dimdwarf.actors._
import net.orfjackal.dimdwarf.net.sgs._
import net.orfjackal.dimdwarf.domain._

@RunWith(classOf[Specsy])
class LoginLogoutSpec extends Spec {
  val queues = new DeterministicMessageQueues
  val authenticator = new FakeAuthenticator
  val clock = new Clock(new SimpleTimestamp(0L))
  val networkActor = new DummyNetworkActor()

  val toNetwork = new MessageQueue[NetworkMessage]("toNetwork")
  queues.addActor(networkActor, toNetwork)
  val networkCtrl = new NetworkController(toNetwork, authenticator, null, clock)
  queues.addController(networkCtrl)

  val USERNAME = "John Doe"
  val PASSWORD = "secret"
  val WRONG_PASSWORD = "wrong-password"
  val SESSION = DummySessionHandle(1)


  "When client send a login request with right credentials" >> {
    clientSends(LoginRequest(USERNAME, PASSWORD))

    "NetworkController sends a success message to the client" >> {
      assertMessageSent(toNetwork, SendToClient(LoginSuccess(), SESSION))
    }
  }

  "When client send a login request with wrong credentials" >> {
    clientSends(LoginRequest(USERNAME, WRONG_PASSWORD))

    "NetworkController sends a failure message to the client" >> {
      assertMessageSent(toNetwork, SendToClient(LoginFailure(), SESSION))
    }
  }

  "When client sends a logout request" >> {
    clientSends(LoginRequest(USERNAME, PASSWORD))
    clientSends(LogoutRequest())

    // TODO: keep track of which clients are connected (cam be test-driven with JMX monitoring or session messages)
    "and NetworkController logs out the client"

    "after which NetworkController sends a logout success message to the client" >> {
      assertMessageSent(toNetwork, SendToClient(LogoutSuccess(), SESSION))
    }
  }

  // TODO: when a client is not logged in, do not allow a logout request (or any other messages)

  private def assertMessageSent(queue: MessageQueue[NetworkMessage], expected: Any) {
    assertThat(queues.seenIn(queue).last, is(expected))
  }

  private def clientSends(message: ClientMessage) {
    queues.toHub.send(ReceivedFromClient(message, SESSION))
    queues.processMessagesUntilIdle()
  }

  class FakeAuthenticator extends Authenticator {
    def isUserAuthenticated(credentials: Credentials, onYes: => Unit, onNo: => Unit) {
      if (credentials == new PasswordCredentials(USERNAME, PASSWORD)) {
        onYes
      } else {
        onNo
      }
      queues.processMessagesUntilIdle()
    }
  }

  class DummyNetworkActor extends Actor[NetworkMessage] {
    def start() {}

    def process(message: NetworkMessage) {}
  }

  case class DummySessionHandle(id: Int) extends SessionHandle
}
