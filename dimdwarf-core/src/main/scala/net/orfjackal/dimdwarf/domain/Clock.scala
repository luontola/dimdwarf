package net.orfjackal.dimdwarf.domain

import java.util.concurrent.atomic.AtomicReference
import javax.annotation.concurrent.ThreadSafe

@ThreadSafe
class Clock(startingValue: Timestamp) {
  private val next = new AtomicReference[Timestamp](startingValue)

  def nextTimestamp(): Timestamp = {
    while (true) {
      val current = next.get
      if (next.compareAndSet(current, current.next)) {
        return current
      }
    }
    throw new AssertionError("unreachable line")
  }
}
