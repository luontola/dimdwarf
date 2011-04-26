package net.orfjackal.dimdwarf.net.sgs

import org.hamcrest.Matchers._
import org.hamcrest.MatcherAssert.assertThat
import org.junit.runner.RunWith
import net.orfjackal.specsy._
import org.apache.mina.core.session._
import org.mockito.Mockito._
import org.apache.mina.filter.codec._
import SimpleSgsProtocolReferenceMessages._
import net.orfjackal.dimdwarf.db.Blob

@RunWith(classOf[Specsy])
class SimpleSgsProtocolSpec extends Spec {
  val session = new DummySession
  val decoded = mock(classOf[ProtocolDecoderOutput])
  val encoded = mock(classOf[ProtocolEncoderOutput])

  val encoder = new SimpleSgsProtocolEncoder
  val decoder = new SimpleSgsProtocolDecoder

  assertThat(true, is(true)) // TODO (this line is here just to keep the imports in place)

  "Decode LOGIN_REQUEST" >> {
    val in = loginRequest("username", "password")

    decoder.decode(session, in, decoded)

    verify(decoded).write(LoginRequest("username", "password"))
  }

  "Encode LOGIN_SUCCESS" >> {
    val message = LoginSuccess()

    encoder.encode(session, message, encoded)

    verify(encoded).write(loginSuccess(new Array[Byte](0))) // TODO: add reconnectionKey
  }

  "Encode LOGIN_FAILURE" >> {
    val message = LoginFailure()

    encoder.encode(session, message, encoded)

    verify(encoded).write(loginFailure("")) // TODO: add reason
  }

  "Decode SESSION_MESSAGE" >> {
    val bytes = Array[Byte](1, 2, 3)
    val in = sessionMessage(bytes)

    decoder.decode(session, in, decoded)

    verify(decoded).write(SessionMessage(Blob.fromBytes(bytes)))
  }

  "Decode LOGOUT_REQUEST" >> {
    val in = logoutRequest()

    decoder.decode(session, in, decoded)

    verify(decoded).write(LogoutRequest())
  }
}
