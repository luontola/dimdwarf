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

    val sessionMessagesToSend = messageService.sessionMessagesToSend.toList map {(message) => (sessionId, message)}
    val commit = TransactionCommitRequest(sessionMessagesToSend)
    toHub.send(commit)
  }

  def commitTransaction(commit: TransactionCommitRequest) {
    // XXX: do not call this method from NetworkController, but a more appropriate place, or make TaskExecutor itself a controller?
    commit.sessionMessagesToSend foreach {
      case (sessionId, message) =>
        toHub.send(SessionMessageToClient(message, sessionId))
    }
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

case class TransactionCommitRequest(sessionMessagesToSend: List[(SessionId, Blob)])
