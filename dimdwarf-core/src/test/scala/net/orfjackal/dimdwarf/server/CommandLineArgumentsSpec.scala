package net.orfjackal.dimdwarf.server

import org.junit.runner.RunWith
import net.orfjackal.specsy._
import org.hamcrest.Matchers._
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert._
import org.kohsuke.args4j._
import java.io._

@RunWith(classOf[Specsy])
class CommandLineArgumentsSpec extends Spec {
  // Some documentation on how to use the args4j library
  // http://args4j.java.net/
  // http://weblogs.java.net/blog/kohsuke/archive/2005/05/parsing_command.html

  val options = new ServerOptions()
  val parser = new CmdLineParser(options)

  "Parses all command line arguments" >> {
    parser.parseArgument(
      "--app", "path/to/app",
      "--port", "1000")

    assertThat(options.applicationDir, is(new File("path/to/app")))
    assertThat(options.port, is(1000))
  }

  "Fails if some required arguments are missing" >> {
    try {
      parser.parseArgument()
      fail("expected to throw an exception")
    } catch {
      case e: CmdLineException =>
        assertThat(e.getMessage, is("Option \"--app\" is required"))
    }
  }

  "Shows a help message" >> {
    assertThat(parser.printExample(ExampleMode.REQUIRED), is(" --app FILE --port N"))

    val out = new StringWriter()
    parser.printUsage(out, null)
    val usage = out.toString
    assertThat(usage, containsString("--app FILE"))
    assertThat(usage, containsString("path to the application directory"))
  }
}
