import sbt._

class DimdwarfProject(info: ProjectInfo) extends DefaultProject(info) {
  val mavenLocal = "Local Maven Repository" at "file://" + Path.userHome + "/.m2/repository"
  val laughingPanda = "Laughing Panda Maven Repository" at "http://www.laughingpanda.org/maven2"
  //val google = "Google Maven2" at "http://google-maven-repository.googlecode.com/svn/repository"
  
  lazy val agent = project("dimdwarf-agent", "dimdwarf-agent", new DefaultProject(_))
  lazy val aop = project("dimdwarf-aop", "dimdwarf-aop", new DefaultProject(_))
  lazy val api = project("dimdwarf-api", "dimdwarf-api", new DefaultProject(_))
  lazy val apiInternal = project("dimdwarf-api-internal", "dimdwarf-api-internal", new DefaultProject(_))
  lazy val core = project("dimdwarf-core", "dimdwarf-core", new DefaultProject(_))
  lazy val dist = project("dimdwarf-dist", "dimdwarf-dist", new DefaultProject(_))
  lazy val endToEndTest = project("dimdwarf-end-to-end-test", "dimdwarf-end-to-end-test", new DefaultProject(_))
}