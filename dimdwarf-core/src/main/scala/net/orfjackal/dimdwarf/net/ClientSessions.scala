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

  def process(session: SessionHandle, event: SessionState => Transition) {
    val oldState = sessionStates.getOrElse(session, new NewConnection(session))
    val (newState, actions) = event(oldState)
    if (newState.isInstanceOf[Disconnected]) {
      // TODO: release all resources here?
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

    protected def stayAsIs(action: => Unit): Transition = become(this)(action)

    protected def become(newState: SessionState)(action: => Unit): Transition = (newState, () => action)

    protected def LoggingIn: SessionState = new LoggingIn(session)

    protected def Authenticated: SessionState = new Authenticated(session)

    protected def LoggingOut: SessionState = new LoggingOut(session)

    protected def Disconnected: SessionState = new Disconnected(session)
  }

  /**
   * Transient initial state; needed only to handle the `onConnected` event
   * in a similar fashion as all other events.
   */
  private class NewConnection(session: SessionHandle) extends SessionState(session) {
    override def onConnected() = become(LoggingIn) {
      val sessionId = sessionIdFactory.uniqueSessionId()
      sessionIds(session) = sessionId
    }
  }

  /**
   * Client connected, but not yet authenticated.
   */
  private class LoggingIn(session: SessionHandle) extends SessionState(session) {
    override def onDisconnected() = become(Disconnected) {
      // FIXME: sessionIds is not cleared in all possible states; do this in `process` or the base class?
      sessionIds -= session
    }

    override def onLoginRequest(credentials: Credentials, authenticator: Authenticator) = stayAsIs {
      authenticator.isUserAuthenticated(credentials,
        onYes = process(session, _.onLoginSuccess()),
        onNo = process(session, _.onLoginFailure()))
    }

    override protected[ClientSessions] def onLoginSuccess() = become(Authenticated) {
      notifier.fireLoginSuccess(session)
    }

    // TODO: should become LoggingOut? or should allow relogin?
    override protected[ClientSessions] def onLoginFailure() = become(Disconnected) {
      notifier.fireLoginFailure(session)
    }
  }

  /**
   * Allow regular operation.
   */
  private class Authenticated(session: SessionHandle) extends SessionState(session) {
    override def onSessionMessage(message: Blob, taskExecutor: TaskExecutor) = stayAsIs {
      taskExecutor.processSessionMessage(session, message)
    }

    override def onLogoutRequest() = become(LoggingOut) {
      notifier.fireLogoutSuccess(session)
    }
  }

  /**
   * It's the client's responsibility to disconnect after the client receives
   * the logout success message. In this state, wait for the client to disconnect,
   * or after a timeout make the server disconnect.
   */
  private class LoggingOut(session: SessionHandle) extends SessionState(session) {
    override def onDisconnected() = become(Disconnected) {
      // TODO: should this `onDisconnected` be in the base class, to inherit it to all states?
    }
  }

  /**
   * Terminal state; no further events allowed. Release all resources.
   */
  private class Disconnected(session: SessionHandle) extends SessionState(session) {
    // TODO: make `onDisconnected` log an error, if the default in base class is made to disconnect quietly? 
  }
}
