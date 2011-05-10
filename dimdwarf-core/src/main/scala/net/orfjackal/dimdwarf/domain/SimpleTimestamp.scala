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

  private def unsignedCompare(a: Long, b: Long) = unsigned(a).compareTo(unsigned(b))

  private def unsigned(x: Long) = x + Long.MinValue

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
