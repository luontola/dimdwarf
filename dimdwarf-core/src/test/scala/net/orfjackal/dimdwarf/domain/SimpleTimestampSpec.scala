package net.orfjackal.dimdwarf.domain

import org.junit.runner.RunWith
import net.orfjackal.specsy._
import org.junit.Assert._
import org.hamcrest.Matchers._
import org.hamcrest.MatcherAssert.assertThat

@RunWith(classOf[Specsy])
class SimpleTimestampSpec extends Spec {
  "Timestamps are value objects" >> {
    val ts1a = SimpleTimestamp(1L)
    val ts1b = SimpleTimestamp(1L)
    val ts2 = SimpleTimestamp(2L)

    assertThat(ts1a, equalTo(ts1b))
    assertThat(ts1a, not(equalTo(ts2)))
    assertThat(ts2, not(equalTo(ts1a)))
    assertThat(ts1a.hashCode, equalTo(ts1b.hashCode))
    assertThat(ts1a.hashCode, not(equalTo(ts2.hashCode)))
  }

  "Timestamps are shown in hexadecimal format" >> {
    assertThat(SimpleTimestamp(0L).toString, is("{00000000-00000000}"))
    assertThat(SimpleTimestamp(1L).toString, is("{00000000-00000001}"))
    assertThat(SimpleTimestamp(Long.MaxValue).toString, is("{7fffffff-ffffffff}"))
    assertThat(SimpleTimestamp(Long.MinValue).toString, is("{80000000-00000000}"))
    assertThat(SimpleTimestamp(-1L).toString, is("{ffffffff-ffffffff}"))
  }

  "Timestamps can be incremented" >> {
    assertThat(SimpleTimestamp(0L).next, is(SimpleTimestamp(1L)))
    assertThat(SimpleTimestamp(1L).next, is(SimpleTimestamp(2L)))
    assertThat(SimpleTimestamp(Long.MaxValue).next, is(SimpleTimestamp(Long.MinValue)))
    assertThat(SimpleTimestamp(-2L).next, is(SimpleTimestamp(-1L)))
  }

  "Timestamps cannot overflow" >> {
    try {
      SimpleTimestamp(-1L).next

      fail("should have thrown an exception")
    } catch {
      case e: IllegalStateException =>
        assertThat(e.getMessage, containsString("overflow"))
    }
  }

  "Timestamps are ordered" >> {
    assertComparableInOrder(
      SimpleTimestamp(0L),
      SimpleTimestamp(1L),
      SimpleTimestamp(Int.MaxValue - 1L),
      SimpleTimestamp(Int.MaxValue),
      SimpleTimestamp(Int.MaxValue + 1L),
      SimpleTimestamp(Long.MaxValue - 1L),
      SimpleTimestamp(Long.MaxValue),
      SimpleTimestamp(Long.MinValue),
      SimpleTimestamp(Long.MinValue + 1L),
      SimpleTimestamp(Int.MinValue - 1L),
      SimpleTimestamp(Int.MinValue),
      SimpleTimestamp(Int.MinValue + 1L),
      SimpleTimestamp(-1L))
  }

  private def assertComparableInOrder(ordered: Timestamp*) {
    shareSideEffects() // performance optimization

    for (i <- 0 until ordered.length) {
      val lesser = ordered(i)
      assertEqualToItself(lesser)

      for (j <- i + 1 until ordered.length) {
        val greater = ordered(j)

        // TODO: use sharing of side-effects, once implemented in Specsy, to improve performance
        "Case: " + lesser + " < " + greater >> {
          assertLessThan(lesser, greater)
        }
      }
    }
  }

  private def assertEqualToItself(ts: Timestamp) {
    assertTrue("did not satisfy: " + ts + " == " + ts, ts.compareTo(ts) == 0)
  }

  private def assertLessThan(lesser: Timestamp, greater: Timestamp) {
    assertTrue("did not satisfy: " + lesser + " < " + greater, lesser.compareTo(greater) < 0)
    assertTrue("did not satisfy: " + greater + " > " + lesser, greater.compareTo(lesser) > 0)
  }
}
