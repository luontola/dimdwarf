package net.orfjackal.dimdwarf.tasks2

import net.orfjackal.dimdwarf.db.Blob
import net.orfjackal.dimdwarf.mq.MessageSender
import javax.inject.Inject
import net.orfjackal.dimdwarf.controller._
import net.orfjackal.dimdwarf.domain._
import scala.collection.mutable.Buffer
import java.nio.ByteBuffer
import net.orfjackal.dimdwarf.api._

@ControllerScoped
class TaskExecutor @Inject()(@Hub toHub: MessageSender[Any]) {
  // TODO: do not pass toNetwork to this class, but keep it hidden inside NetworkController

  def processSessionMessage(sessionId: SessionId, message: Blob) {

    // TODO: handle the session message in a worker thread
    val messageService = new FakeMessageService
    val application = new EchoClientListener(new FakeSession(sessionId, messageService))
    application.onSessionMessage(message.toByteBuffer)

    // TODO: use asynchronous commit request messages

    // TODO: on commit, send session messages to clients
    messageService.sessionMessagesToSend.foreach(m => toHub.send(SessionMessageToClient(m, sessionId)))
  }


  class EchoClientListener(currentSession: Session) extends ClientListener {
    def onSessionMessage(message: ByteBuffer) {
      currentSession.send(message)
    }
  }

  class FakeMessageService {
    val sessionMessagesToSend = Buffer[Blob]()

    def sendSessionMessage(sessionId: SessionId, message: ByteBuffer) {
      sessionMessagesToSend.append(Blob.fromByteBuffer(message))
    }
  }

  class FakeSession(sessionId: SessionId, messageService: FakeMessageService) extends Session {
    def send(message: ByteBuffer) {
      messageService.sendSessionMessage(sessionId, message)
    }
  }
}
