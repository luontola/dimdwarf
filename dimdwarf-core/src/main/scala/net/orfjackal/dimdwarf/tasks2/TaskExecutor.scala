package net.orfjackal.dimdwarf.tasks2

import net.orfjackal.dimdwarf.db.Blob
import net.orfjackal.dimdwarf.mq.MessageSender
import javax.inject.Inject
import net.orfjackal.dimdwarf.controller._
import net.orfjackal.dimdwarf.domain._

@ControllerScoped
class TaskExecutor @Inject()(@Hub toHub: MessageSender[Any]) {
  // TODO: do not pass toNetwork to this class, but keep it hidden inside NetworkController

  def processSessionMessage(sessionId: SessionId, message: Blob) {
    // TODO: handle the session message in a worker thread

    // TODO: use application's client session listener
    val committedSessionMessages = List(message)

    // TODO: on commit, send session messages to clients
    committedSessionMessages.foreach(m => toHub.send(SessionMessageToClient(m, sessionId)))
  }
}
