package net.orfjackal.dimdwarf.domain

import org.junit.runner.RunWith
import net.orfjackal.specsy._
import org.hamcrest.Matchers._
import org.hamcrest.MatcherAssert.assertThat

@RunWith(classOf[Specsy])
class SessionIdFactorySpec extends Spec {
  val factory = new SessionIdFactory(new Clock(SimpleTimestamp(0L)))

  "Generates unique session IDs" >> {
    val ids = (1 to 10).map(_ => factory.uniqueSessionId())

    assertThat(ids.distinct.size, is(ids.size))
  }
}
