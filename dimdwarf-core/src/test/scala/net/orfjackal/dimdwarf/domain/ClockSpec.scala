// Copyright © 2008-2012 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://dimdwarf.sourceforge.net/LICENSE

package net.orfjackal.dimdwarf.domain

import org.hamcrest.Matchers._
import org.hamcrest.MatcherAssert.assertThat
import java.util.ArrayList
import java.util.concurrent._
import org.specsy.scala.ScalaSpecsy

class ClockSpec extends ScalaSpecsy {
  "Clock creates incrementing timestamps starting from the first value" >> {
    val clock = new Clock(SimpleTimestamp(0L))

    assertThat(clock.nextTimestamp(), is(SimpleTimestamp(0L): Timestamp))
    assertThat(clock.nextTimestamp(), is(SimpleTimestamp(1L): Timestamp))
    assertThat(clock.nextTimestamp(), is(SimpleTimestamp(2L): Timestamp))
  }

  "Clock is thread safe" >> {
    import scala.collection.JavaConversions._
    val clock = new Clock(SimpleTimestamp(0L))

    val tasks = new ArrayList[Callable[Timestamp]]
    for (i <- 1 to 100) {
      tasks.add(new Callable[Timestamp] {
        def call() = clock.nextTimestamp()
      })
    }
    val executor = Executors.newFixedThreadPool(10)
    defer {executor.shutdown()}
    val timestamps = executor.invokeAll(tasks).map(_.get)

    assertThat("all values should be unique", timestamps.distinct.size, is(timestamps.size))
    assertThat("no values should be skipped", clock.nextTimestamp(), is(SimpleTimestamp(100L): Timestamp))
  }
}
