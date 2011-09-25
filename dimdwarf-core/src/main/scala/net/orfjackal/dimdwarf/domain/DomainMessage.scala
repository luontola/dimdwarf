package net.orfjackal.dimdwarf.domain

import net.orfjackal.dimdwarf.db.Blob

abstract sealed class DomainMessage

case class SessionMessageToClient(message: Blob, sessionId: SessionId)
