package net.orfjackal.dimdwarf.net

import net.orfjackal.dimdwarf.mq.MessageSender
import net.orfjackal.dimdwarf.controller._
import net.orfjackal.dimdwarf.auth._
import net.orfjackal.dimdwarf.net.sgs._
import javax.inject.Inject
import net.orfjackal.dimdwarf.tasks2.TaskExecutor
import net.orfjackal.dimdwarf.domain._

// TODO: rename to ClientConnectionController or ClientSessionController?
@ControllerScoped
class NetworkController @Inject()(toNetwork: MessageSender[NetworkMessage],
                                  authenticator: Authenticator,
                                  taskExecutor: TaskExecutor,
                                  clock: Clock) extends Controller with ClientSessionNotifier {
  private val sessions = new ClientSessions(clock, this)

  def process(message: Any) {
    message match {
      case ReceivedFromClient(message, session) =>
        processClientMessage(message, session)

      case SessionMessageToClient(message, sessionId) =>
        // TODO: write a unit test for this (and in multinode it may need to forward this message to another server node)
        // TODO: should also this be done in ClientSessions?
        val session = sessions.getSessionHandle(sessionId)
        sendToClient(session, SessionMessage(message))

      case _ =>
    }
  }

  // TODO: have events for when a client connects/disconnects at the socket level?
  // - org.apache.mina.core.service.IoHandler.sessionOpened
  // - org.apache.mina.core.service.IoHandler.sessionClosed

  // TODO: add a way for the server to explicitly disconnect the client
  // (on logout using a timeout? on illegal message immediately)

  private def processClientMessage(message: ClientMessage, session: SessionHandle) {
    message match {
      case LoginRequest(username, password) =>
        sessions.process(session, _.onConnected())
        sessions.process(session, _.onLoginRequest(new PasswordCredentials(username, password), authenticator))

      case SessionMessage(message) =>
        sessions.process(session, _.onSessionMessage(message, taskExecutor))

      case LogoutRequest() =>
        sessions.process(session, _.onLogoutRequest())

      case _ =>
        // TODO: do something smart, maybe disconnect the client if it sends a not allowed message
        assert(false, "Unsupported message: " + message)
    }
  }

  def fireLoginSuccess(session: SessionHandle) {
    sendToClient(session, LoginSuccess())
  }

  def fireLoginFailure(session: SessionHandle) {
    sendToClient(session, LoginFailure())
  }

  def fireLogoutSuccess(session: SessionHandle) {
    sendToClient(session, LogoutSuccess())
  }

  private def sendToClient(session: SessionHandle, message: ClientMessage) {
    toNetwork.send(SendToClient(message, session))
  }
}
