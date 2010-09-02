import sbt._

class DimdwarfProject(info: ProjectInfo) extends DimdwarfParentProject(info) {
  //val mavenLocal = "Local Maven Repository" at "file://" + Path.userHome + "/.m2/repository"
  //val laughingPanda = "Laughing Panda Maven Repository" at "http://www.laughingpanda.org/maven2"
  //val google = "Google Maven2" at "http://google-maven-repository.googlecode.com/svn/repository"

  lazy val aop = project("dimdwarf-aop", "dimdwarf-aop", new DimdwarfAop(_), parent)
  lazy val api = project("dimdwarf-api", "dimdwarf-api", new DimdwarfApi(_), parent)
  lazy val apiInternal = project("dimdwarf-api-internal", "dimdwarf-api-internal", new DimdwarfApiInternal(_), api, parent)
  lazy val core = project("dimdwarf-core", "dimdwarf-core", new DimdwarfCore(_), api, apiInternal, aop, parent)

  lazy val distModules = project("dist", "dist", new DistModules(_))
  lazy val endToEndTests = project("end-to-end-tests", "end-to-end-tests", new EndToEndTests(_), distModules.dist, parent)
  lazy val parent = project("parent", "parent", new Parent(_))

  class DimdwarfAop(info: ProjectInfo) extends DimdwarfDefaultProject(info)
  class DimdwarfApi(info: ProjectInfo) extends DimdwarfDefaultProject(info)
  class DimdwarfApiInternal(info: ProjectInfo) extends DimdwarfDefaultProject(info)
  class DimdwarfCore(info: ProjectInfo) extends DimdwarfDefaultProject(info)

  class DistModules(info: ProjectInfo) extends DimdwarfParentProject(info) {
    lazy val agent = project("agent", "agent", new Agent(_), parent)
    lazy val launcher = project("launcher", "launcher", new Launcher(_), parent)
    lazy val dist = project("dimdwarf-dist", "dimdwarf-dist", new DimdwarfDist(_), core, agent, launcher, parent)

    class Agent(info: ProjectInfo) extends DimdwarfDefaultProject(info)
    class Launcher(info: ProjectInfo) extends DimdwarfDefaultProject(info)
    class DimdwarfDist(info: ProjectInfo) extends DimdwarfDefaultProject(info)
  }
  class EndToEndTests(info: ProjectInfo) extends DimdwarfDefaultProject(info)
  class Parent(info: ProjectInfo) extends DimdwarfDefaultProject(info)
}

trait BasicDimdwarfProject extends BasicDependencyProject {
  val mavenLocal = "Local Maven Repository" at (Path.userHome / ".m2" / "repository").asURL.toString
}

abstract class DimdwarfParentProject(info: ProjectInfo) extends ParentProject(info) with BasicDimdwarfProject
abstract class DimdwarfDefaultProject(info: ProjectInfo) extends DefaultProject(info) with BasicDimdwarfProject
