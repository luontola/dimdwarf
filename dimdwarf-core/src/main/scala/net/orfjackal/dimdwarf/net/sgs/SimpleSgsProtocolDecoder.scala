package net.orfjackal.dimdwarf.net.sgs

import org.apache.mina.core.buffer.IoBuffer
import com.sun.sgs.protocol.simple.SimpleSgsProtocol
import org.apache.mina.filter.codec._
import org.apache.mina.core.session.IoSession
import javax.annotation.concurrent.Immutable
import java.nio.charset.Charset
import net.orfjackal.dimdwarf.db.Blob

@Immutable
class SimpleSgsProtocolDecoder extends CumulativeProtocolDecoder {
  private val stringCharset = Charset.forName("UTF-8")

  protected def doDecode(session: IoSession, in: IoBuffer, out: ProtocolDecoderOutput): Boolean = {
    if (in.prefixedDataAvailable(2, SimpleSgsProtocol.MAX_PAYLOAD_LENGTH)) {
      val payloadLength = readUnsignedShort(in)
      val op = readByte(in)

      val message = op match {
        case SimpleSgsProtocol.LOGIN_REQUEST =>
          val version = readByte(in)
          require(version == SimpleSgsProtocol.VERSION, "incompatible version: " + version)
          val username = readString(in)
          val password = readString(in)
          LoginRequest(username, password)

        case SimpleSgsProtocol.SESSION_MESSAGE =>
          val message = readBytes(payloadLength - 1, in)
          SessionMessage(message)

        case SimpleSgsProtocol.LOGOUT_REQUEST =>
          LogoutRequest()
      }

      out.write(message)
      true
    } else {
      false
    }
  }

  private def readUnsignedShort(in: IoBuffer): Int = in.getUnsignedShort()

  private def readByte(in: IoBuffer): Byte = in.get()

  private def readString(in: IoBuffer): String = in.getPrefixedString(2, stringCharset.newDecoder)

  private def readBytes(length: Int, in: IoBuffer): Blob = {
    val bytes = new Array[Byte](length)
    in.get(bytes)
    Blob.fromBytes(bytes)
  }
}
