package net.orfjackal.dimdwarf.domain

import net.orfjackal.dimdwarf.net.SessionHandle
import net.orfjackal.dimdwarf.db.Blob

abstract sealed class DomainMessage

// TODO: do not refer SessionHandle, use SessionId
case class SessionMessageToClient(message: Blob, session: SessionHandle)
