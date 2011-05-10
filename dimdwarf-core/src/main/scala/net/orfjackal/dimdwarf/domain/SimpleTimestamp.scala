package net.orfjackal.dimdwarf.domain

case class SimpleTimestamp(timestamp: Long) extends Timestamp {
  def compareTo(o: Timestamp): Int = {
    val that = o.asInstanceOf[SimpleTimestamp]
    if (inUpperRange(this.timestamp) && inLowerRange(that.timestamp)) {
      1
    } else if (inLowerRange(this.timestamp) && inUpperRange(that.timestamp)) {
      -1
    } else {
      this.timestamp.compareTo(that.timestamp)
    }
  }

  private def inLowerRange(value: Long) = value >= 0

  private def inUpperRange(value: Long) = value < 0

  override def toString: String = {
    "{" + separators(8, padWithZero(16, timestamp.toHexString)) + "}"
  }

  private def padWithZero(length: Int, hex: String): String = {
    ("0" * (length - hex.length)) + hex
  }

  private def separators(length: Int, hex: String): String = {
    val (front, back) = hex.splitAt(length)
    front + "-" + back
  }
}
