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

@RunWith(classOf[Specsy])
class ClientSessionsSpec extends Spec with Assertions {
  val clock = new Clock(SimpleTimestamp(0L))
  val notifier = mock(classOf[ClientSessionNotifier])
  val sessions = new ClientSessions(clock, notifier)

  val session1 = DummySessionHandle(1)
  val session2 = DummySessionHandle(2)

  // TODO: rewrite these tests to cover all states and events

  "Each connected client gets its own session ID" >> {
    sessions.onConnected(session1)
    sessions.onConnected(session2)
    val id1 = sessions.getSessionId(session1)
    val id2 = sessions.getSessionId(session2)

    assertThat(id1, is(not(id2)))
    assertThat(sessions.count, is(2))
  }
  "Sessions are remembered while the client is connected" >> {
    sessions.onConnected(session1)
    val id1a = sessions.getSessionId(session1)
    val id1b = sessions.getSessionId(session1)

    assertThat(id1a, is(id1b))
    assertThat(sessions.count, is(1))
  }
  "Sessions are forgotten when the client disconnects" >> {
    sessions.onConnected(session1)
    sessions.onDisconnected(session1)

    intercept[AssertionError] {
      sessions.getSessionId(session1)
    }
    assertThat(sessions.count, is(0))
  }
  "Connecting twice is disallowed" >> {
    sessions.onConnected(session1)

    intercept[IllegalStateException] {
      sessions.onConnected(session1)
    }
  }
  "Disconnecting when disconnected is disallowed" >> {
    intercept[IllegalStateException] {
      sessions.onDisconnected(session1)
    }
  }

  "Client sending session messages to the server" >> {
    val message = Blob.fromBytes("hello".getBytes)
    val taskExecutor = mock(classOf[TaskExecutor]) // XXX: TaskExecutor should be an interface

    "Disallowed when disconnected" >> {
      intercept[IllegalStateException] {
        sessions.onSessionMessage(session1, message, taskExecutor)
      }

      verifyZeroInteractions(taskExecutor)
    }
    "Disallowed when connected" >> {
      sessions.onConnected(session1)
      intercept[IllegalStateException] {
        sessions.onSessionMessage(session1, message, taskExecutor)
      }

      verifyZeroInteractions(taskExecutor)
    }
    "Allowed when authenticated" >> {
      sessions.onConnected(session1)
      sessions.onLoginSuccess(session1)
      sessions.onSessionMessage(session1, message, taskExecutor)

      verify(taskExecutor).processSessionMessage(session1, message)
    }
  }

  case class DummySessionHandle(id: Int) extends SessionHandle
}
