package net.orfjackal.dimdwarf.net

import net.orfjackal.dimdwarf.domain._
import scala.collection._
import net.orfjackal.dimdwarf.db.Blob
import net.orfjackal.dimdwarf.tasks2.TaskExecutor
import net.orfjackal.dimdwarf.auth._

class ClientSessions(clock: Clock, notifier: ClientSessionNotifier) {
  private val sessionIdFactory = new SessionIdFactory(clock)

  private val sessionIds = mutable.Map[SessionHandle, SessionId]()
  private val sessionStates = mutable.Map[SessionHandle, SessionState]()

  def process(session: SessionHandle, f: SessionState => Transition) {
    val oldState = sessionStates.getOrElse(session, new Disconnected(session))
    val (newState, actions) = f(oldState)
    if (newState.isInstanceOf[Disconnected]) {
      sessionStates -= session
    } else {
      sessionStates(session) = newState
    }
    // Executed after changing the state, to allow asynchronous actions
    // to be executed synchronously in unit tests (e.g. authentication)
    actions()
  }

  def getSessionId(session: SessionHandle): SessionId = {
    assert(sessionIds.contains(session), "client not connected")
    sessionIds(session)
  }

  def count: Int = {
    assert(sessionStates.size == sessionIds.size)
    sessionStates.size
  }

  type Transition = (SessionState, () => Unit)

  abstract sealed class SessionState(session: SessionHandle) {
    // TODO: log illegal events and disconnect the client (needs to fire a disconnect request?)
    // - might need an end-to-end test for checking illegal client behavior

    def onConnected(): Transition = operationNotAllowed

    def onDisconnected(): Transition = operationNotAllowed

    def onLoginRequest(credentials: Credentials, authenticator: Authenticator): Transition = operationNotAllowed

    protected[ClientSessions] def onLoginSuccess(): Transition = operationNotAllowed

    protected[ClientSessions] def onLoginFailure(): Transition = operationNotAllowed

    def onSessionMessage(message: Blob, taskExecutor: TaskExecutor): Transition = operationNotAllowed

    def onLogoutRequest(): Transition = operationNotAllowed

    // TODO: disconnect the client when it tries to do an illegal operation
    private def operationNotAllowed: Nothing =
      throw new IllegalStateException("operation not allowed in state " + getClass.getSimpleName)


    protected def stayAsIs: SessionState = this

    protected def becomeDisconnected: SessionState = new Disconnected(session)

    protected def becomeNotAuthenticated: SessionState = new NotAuthenticated(session)

    protected def becomeAuthenticated: SessionState = new Authenticated(session)
  }


  private class Disconnected(session: SessionHandle) extends SessionState(session) {
    override def onConnected() = (becomeNotAuthenticated, () => {
      val sessionId = sessionIdFactory.uniqueSessionId()
      sessionIds(session) = sessionId
    })
  }


  private class NotAuthenticated(session: SessionHandle) extends SessionState(session) {
    override def onDisconnected() = (becomeDisconnected, () => {
      sessionIds -= session
    })

    override def onLoginRequest(credentials: Credentials, authenticator: Authenticator) =
      (stayAsIs, () => {
        authenticator.isUserAuthenticated(credentials,
          onYes = process(session, _.onLoginSuccess()),
          onNo = process(session, _.onLoginFailure()))
      })

    override protected[ClientSessions] def onLoginSuccess() = (becomeAuthenticated, () => {
      notifier.fireLoginSuccess(session)
    })

    override protected[ClientSessions] def onLoginFailure() = (becomeDisconnected, () => {
      notifier.fireLoginFailure(session)
    })
  }


  private class Authenticated(session: SessionHandle) extends SessionState(session) {
    override def onSessionMessage(message: Blob, taskExecutor: TaskExecutor) =
      (stayAsIs, () => {
        taskExecutor.processSessionMessage(session, message)
      })

    override def onLogoutRequest() = (becomeDisconnected, () => {
      // TODO: transition to a new state "Disconnecting" or "LoggingOut", because it's the client's responsibility to disconnect?
      notifier.fireLogoutSuccess(session)
    })
  }
}
