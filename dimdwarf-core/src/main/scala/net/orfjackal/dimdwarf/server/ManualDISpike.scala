package net.orfjackal.dimdwarf.server

import com.google.inject._
import net.orfjackal.dimdwarf.auth._
import net.orfjackal.dimdwarf.actors._
import net.orfjackal.dimdwarf.net._
import net.orfjackal.dimdwarf.controller._
import net.orfjackal.dimdwarf.modules.ControllerModule
import javax.inject.Provider
import net.orfjackal.dimdwarf.mq._
import net.orfjackal.dimdwarf.context._
import java.util._

class ManualDISpike {
  def configureServer(port: Int, appModule: Module): ActorStarter = {

    // interface with the application using Guice
    // (individually give access to server objects which the application need to access)
    val appInjector = Guice.createInjector(Stage.PRODUCTION, appModule)
    val credentialsChecker = appInjector.getInstance(classOf[CredentialsChecker[Credentials]])

    // pass the ServerBuilder as a hidden parameter to the actor modules, to make their syntax shorter
    val builder = new ServerBuilder()
    ActorModule2.builder.set(builder)
    try {
      val hub = new ControllerHubModule()
      configureActorModules(credentialsChecker, port, hub.toHub)
      hub.addControllers(builder.controllers)
    } finally {
      ActorModule2.builder.remove()
    }

    new ActorStarter(builder.actors)
  }

  private def configureActorModules(credentialsChecker: CredentialsChecker[Credentials], port: Int, toHub: MessageSender[Any]) {

    // authentication
    val authenticator = new AuthenticatorModule(credentialsChecker, toHub)

    // networking
    val network = new NetworkModule(authenticator.getAuthenticator, port, toHub)
  }
}


// actor infrastructure

class ServerBuilder {
  val controllers = new HashSet[ControllerRegistration]
  val actors = new HashSet[ActorRegistration]

  def registerController(name: String, controller: Controller) {
    controllers.add(new ControllerRegistration(name,
      new StubProvider(controller)))
  }

  def registerActor(name: String, actor: ActorRunnable) {
    actors.add(new ActorRegistration(name,
      new StubProvider(null: Context),
      new StubProvider(actor)))
  }

  private class StubProvider[T](value: T) extends Provider[T] {
    def get(): T = value
  }
}

object ActorModule2 {
  val builder = new ThreadLocal[ServerBuilder]()
}

abstract class ActorModule2[T] {
  private val builder = ActorModule2.builder.get // hidden parameter!
  private val name = getClass.getSimpleName.replace("Module", "")
  private val actorQueue = new MessageQueue[T]("to" + name)

  protected def toActor: MessageSender[T] = actorQueue

  protected def registerController(controller: Controller) {
    builder.registerController(name, controller)
  }

  protected def registerActor(actor: Actor[T]) {
    registerActorRunnable(new ActorMessageLoop(actor, actorQueue))
  }

  protected def registerActorRunnable(actor: ActorRunnable) {
    builder.registerActor(name, actor)
  }
}


// individual actor modules

class ControllerHubModule() extends ActorModule2[Any] {
  val hub = new ControllerHub

  registerActor(hub)

  def toHub: MessageSender[Any] = toActor

  def addControllers(controllers: java.util.Set[ControllerRegistration]) {
    ControllerModule.registerControllers(hub, controllers)
  }
}

class AuthenticatorModule(credentialsChecker: CredentialsChecker[Credentials], toHub: MessageSender[Any]) extends ActorModule2[AuthenticatorMessage] {
  private val controller = new AuthenticatorController(toActor)
  private val actor = new AuthenticatorActor(toHub, credentialsChecker)

  registerController(controller)
  registerActor(actor)

  def getAuthenticator: Authenticator = controller
}

class NetworkModule(authenticator: Authenticator, port: Int, toHub: MessageSender[Any]) extends ActorModule2[NetworkMessage] {
  private val controller = new NetworkController(toActor, authenticator)
  private val actor = new NetworkActor(port, toHub)

  registerController(controller)
  registerActor(actor)
}
