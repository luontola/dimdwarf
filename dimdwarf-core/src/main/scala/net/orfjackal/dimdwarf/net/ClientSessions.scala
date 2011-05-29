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

  private def process(session: SessionHandle, f: SessionState => (SessionState, () => Unit)) {
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

  // TODO: inline methods to remove duplication

  def onConnected(session: SessionHandle) {
    process(session, _.onConnected())
  }

  def onDisconnected(session: SessionHandle) {
    process(session, _.onDisconnected())
  }

  def onLoginRequest(session: SessionHandle, credentials: Credentials, authenticator: Authenticator) {
    process(session, _.onLoginRequest(credentials, authenticator))
  }

  def onLoginSuccess(session: SessionHandle) {
    process(session, _.onLoginSuccess())
  }

  def onLoginFailure(session: SessionHandle) {
    process(session, _.onLoginFailure())
  }

  def onSessionMessage(session: SessionHandle, message: Blob, taskExecutor: TaskExecutor) {
    process(session, _.onSessionMessage(message, taskExecutor))
  }

  def onLogoutRequest(session: SessionHandle) {
    process(session, _.onLogoutRequest())
  }


  // TODO: cover all events and states with unit tests

  private abstract class SessionState {
    // TODO: log illegal events and disconnect the client (needs to fire a disconnect request?)
    def onConnected(): (SessionState, () => Unit) = operationNotAllowed

    def onDisconnected(): (SessionState, () => Unit) = operationNotAllowed

    def onLoginRequest(credentials: Credentials, authenticator: Authenticator): (SessionState, () => Unit) = operationNotAllowed

    def onLoginSuccess(): (SessionState, () => Unit) = operationNotAllowed

    def onLoginFailure(): (SessionState, () => Unit) = operationNotAllowed

    def onSessionMessage(message: Blob, taskExecutor: TaskExecutor): (SessionState, () => Unit) = operationNotAllowed

    def onLogoutRequest(): (SessionState, () => Unit) = operationNotAllowed

    // TODO: extract methods: becomeConnected, becomeAuthenticated, becomeDisconnected

    // TODO: disconnect the client when it tries to do an illegal operation
    private def operationNotAllowed: Nothing =
      throw new IllegalStateException("operation not allowed in state " + getClass.getSimpleName)
  }

  private class Disconnected(session: SessionHandle) extends SessionState {
    override def onConnected() = (new NotAuthenticated(session), () => {
      val sessionId = sessionIdFactory.uniqueSessionId()
      sessionIds(session) = sessionId
    })
  }

  private class NotAuthenticated(session: SessionHandle) extends SessionState {
    override def onDisconnected() = (new Disconnected(session), () => {
      sessionIds -= session
    })

    override def onLoginRequest(credentials: Credentials, authenticator: Authenticator) =
      (this, () => {
        authenticator.isUserAuthenticated(credentials,
          onYes = {ClientSessions.this.onLoginSuccess(session)},
          onNo = {ClientSessions.this.onLoginFailure(session)})
      })

    override def onLoginSuccess() = (new Authenticated(session), () => {
      notifier.fireLoginSuccess(session)
    })

    override def onLoginFailure() = (new Disconnected(session), () => {
      notifier.fireLoginFailure(session)
    })
  }

  private class Authenticated(session: SessionHandle) extends SessionState {
    override def onSessionMessage(message: Blob, taskExecutor: TaskExecutor) = (this, () => {
      taskExecutor.processSessionMessage(session, message)
    })

    override def onLogoutRequest() = (new Disconnected(session), () => {
      // TODO: transition to a new state "Disconnecting", because it's the client's responsibility to disconnect?
      notifier.fireLogoutSuccess(session)
    })
  }
}
