// Copyright © 2008-2012 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://dimdwarf.sourceforge.net/LICENSE

package net.orfjackal.dimdwarf.domain

import org.hamcrest.Matchers._
import org.hamcrest.MatcherAssert.assertThat
import org.specsy.scala.ScalaSpecsy

class SessionIdFactorySpec extends ScalaSpecsy {
  val factory = new SessionIdFactory(new Clock(SimpleTimestamp(0L)))

  "Generates unique session IDs" >> {
    val ids = (1 to 10).map(_ => factory.uniqueSessionId())

    assertThat(ids.distinct.size, is(ids.size))
  }
}
