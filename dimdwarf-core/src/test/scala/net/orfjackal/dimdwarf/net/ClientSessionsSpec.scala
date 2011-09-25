package net.orfjackal.dimdwarf.net

import org.junit.runner.RunWith
import net.orfjackal.specsy._
import org.hamcrest.Matchers._
import org.hamcrest.MatcherAssert.assertThat
import net.orfjackal.dimdwarf.domain._
import org.scalatest.Assertions
import net.orfjackal.dimdwarf.db.Blob
import org.mockito.Mockito._
import net.orfjackal.dimdwarf.tasks2.TaskExecutor
import net.orfjackal.dimdwarf.auth._

@RunWith(classOf[Specsy])
class ClientSessionsSpec extends Spec with Assertions {
  val clock = new Clock(SimpleTimestamp(100L))
  val notifier = mock(classOf[ClientSessionNotifier])
  val sessions = new ClientSessions(clock, notifier)

  val session1 = DummySessionHandle(1)
  val session2 = DummySessionHandle(2)

  // TODO: rewrite these tests to cover all states and events

  "Each connected client gets its own session ID" >> {
    sessions.process(session1, _.onConnected())
    sessions.process(session2, _.onConnected())
    val id1 = sessions.getSessionId(session1)
    val id2 = sessions.getSessionId(session2)

    assertThat(id1, is(not(id2)))
    assertThat(sessions.count, is(2))
  }
  "Sessions are remembered while the client is connected" >> {
    sessions.process(session1, _.onConnected())
    val id1a = sessions.getSessionId(session1)
    val id1b = sessions.getSessionId(session1)

    assertThat(id1a, is(id1b))
    assertThat(sessions.count, is(1))
  }
  "Sessions are forgotten when the client disconnects" >> {
    sessions.process(session1, _.onConnected())
    sessions.process(session1, _.onDisconnected())

    intercept[AssertionError] {
      sessions.getSessionId(session1)
    }
    assertThat(sessions.count, is(0))
  }
  "Connecting twice is disallowed" >> {
    sessions.process(session1, _.onConnected())

    intercept[IllegalStateException] {
      sessions.process(session1, _.onConnected())
    }
  }
  "Disconnecting when disconnected is disallowed" >> {
    intercept[IllegalStateException] {
      sessions.process(session1, _.onDisconnected())
    }
  }

  // TODO: postpone (re)writing these tests
  // - fix the obvious resource leaks (sessionIds)
  // - do an end-to-end test about disconnect-on-misbehaving before writing thorough unit tests
  "Client sending session messages to the server" >> {
    val message = Blob.fromBytes("hello".getBytes)
    val taskExecutor = mock(classOf[TaskExecutor]) // XXX: TaskExecutor should be an interface

    "Disallowed when disconnected" >> {
      intercept[IllegalStateException] {
        sessions.process(session1, _.onSessionMessage(message, taskExecutor))
      }

      verifyZeroInteractions(taskExecutor)
    }
    "Disallowed when connected" >> {
      sessions.process(session1, _.onConnected())
      intercept[IllegalStateException] {
        sessions.process(session1, _.onSessionMessage(message, taskExecutor))
      }

      verifyZeroInteractions(taskExecutor)
    }
    "Allowed when authenticated" >> {
      sessions.process(session1, _.onConnected())
      sessions.process(session1, _.onLoginRequest(null, new FakeAuthenticator))
      sessions.process(session1, _.onSessionMessage(message, taskExecutor))

      val sessionId = SessionId(SimpleTimestamp(100L))
      verify(taskExecutor).processSessionMessage(sessionId, message)
    }
  }

  case class DummySessionHandle(id: Int) extends SessionHandle

  class FakeAuthenticator extends Authenticator {
    def isUserAuthenticated(credentials: Credentials, onYes: => Unit, onNo: => Unit) {
      onYes
    }
  }
}
