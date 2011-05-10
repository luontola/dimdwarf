package net.orfjackal.dimdwarf.domain

import javax.annotation.concurrent.ThreadSafe

@ThreadSafe
class SessionIdFactory(clock: Clock) {
  def uniqueSessionId(): SessionId = {
    SessionId(clock.nextTimestamp())
  }
}
