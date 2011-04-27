package net.orfjackal.dimdwarf.net

import net.orfjackal.dimdwarf.mq.MessageSender
import net.orfjackal.dimdwarf.controller._
import net.orfjackal.dimdwarf.auth._
import net.orfjackal.dimdwarf.net.sgs._
import javax.inject.Inject
import net.orfjackal.dimdwarf.tasks2.TaskExecutor
import net.orfjackal.dimdwarf.domain.SessionMessageToClient

@ControllerScoped
class NetworkController @Inject()(toNetwork: MessageSender[NetworkMessage], authenticator: Authenticator, taskExecutor: TaskExecutor) extends Controller {
  def process(message: Any) {
    message match {
      case ReceivedFromClient(message, session) =>
        processClientMessage(message, session)

      case SessionMessageToClient(message, session) =>
        // TODO: write a unit test for this (and in multinode it may need to forward this message to another server node)
        toNetwork.send(SendToClient(SessionMessage(message), session))

      case _ =>
    }
  }

  private def processClientMessage(message: ClientMessage, session: SessionHandle) {
    message match {
      case LoginRequest(username, password) =>
        authenticator.isUserAuthenticated(new PasswordCredentials(username, password),
          onYes = {toNetwork.send(SendToClient(LoginSuccess(), session))},
          onNo = {toNetwork.send(SendToClient(LoginFailure(), session))})

      case SessionMessage(message) =>
        taskExecutor.processSessionMessage(session, message)

      case LogoutRequest() =>
        // TODO: release any resources related to the client (once there are some resources)
        toNetwork.send(SendToClient(LogoutSuccess(), session))

      case _ =>
        // TODO: do something smart, maybe disconnect the client if it sends a not allowed message
        assert(false, "Unsupported message: " + message)
    }
  }
}
