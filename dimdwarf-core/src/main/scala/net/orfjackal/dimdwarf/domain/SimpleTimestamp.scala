package net.orfjackal.dimdwarf.domain

case class SimpleTimestamp(timestamp: Long) extends Timestamp {
  def next: SimpleTimestamp = {
    if (timestamp == -1L) {
      throw new IllegalStateException("timestamp overflow")
    }
    SimpleTimestamp(timestamp + 1L)
  }

  def compare(other: Timestamp): Int = {
    val that = other.asInstanceOf[SimpleTimestamp]
    unsignedCompare(this.timestamp, that.timestamp)
  }

  private def unsignedCompare(a: Long, b: Long): Int = {
    if (inUpperRange(a) && inLowerRange(b)) {
      1
    } else if (inLowerRange(a) && inUpperRange(b)) {
      -1
    } else {
      a.compareTo(b)
    }
  }

  private def inLowerRange(value: Long) = value >= 0

  private def inUpperRange(value: Long) = value < 0

  override def toString: String = {
    "{" + separators(8, zeroPadded(16, timestamp.toHexString)) + "}"
  }

  private def zeroPadded(length: Int, hex: String): String = {
    "0" * (length - hex.length) + hex
  }

  private def separators(length: Int, hex: String): String = {
    val (front, back) = hex.splitAt(length)
    front + "-" + back
  }
}
