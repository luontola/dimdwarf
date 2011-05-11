package net.orfjackal.dimdwarf.domain

import java.util.concurrent.atomic.AtomicReference
import javax.annotation.concurrent.ThreadSafe
import scala.annotation.tailrec

@ThreadSafe
class Clock(startingValue: Timestamp) {
  private val next = new AtomicReference[Timestamp](startingValue)

  @tailrec final def nextTimestamp(): Timestamp = {
    val current = next.get
    if (next.compareAndSet(current, current.next)) {
      current
    } else {
      nextTimestamp()
    }
  }
}
