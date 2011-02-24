package net.orfjackal.dimdwarf.server

import com.google.inject._
import net.orfjackal.dimdwarf.auth._
import net.orfjackal.dimdwarf.actors._
import java.util.HashSet
import net.orfjackal.dimdwarf.net._
import net.orfjackal.dimdwarf.controller._
import net.orfjackal.dimdwarf.modules.ControllerModule
import javax.inject.Provider
import net.orfjackal.dimdwarf.mq._
import net.orfjackal.dimdwarf.context._

class ManualDISpike {
  def configureServer(port: Int, appModule: Module): ActorStarter = {

    // interface with the application using Guice
    // (individually give access to server objects which the application need to access)
    val appInjector = Guice.createInjector(Stage.PRODUCTION, appModule)
    val credentialsChecker = appInjector.getInstance(classOf[CredentialsChecker[Credentials]])

    // pass the ServerBuilder as a hidden parameter to the actor modules, to make their syntax shorter
    val builder = new ServerBuilder()
    ActorModule2.serverBuilder.set(builder)
    try {
      configureActorModules(credentialsChecker, port)
    } finally {
      ActorModule2.serverBuilder.remove()
    }

    builder.build()
  }

  private def configureActorModules(credentialsChecker: CredentialsChecker[Credentials], port: Int) {

    // authentication
    val authenticator = new AuthenticatorModule(credentialsChecker)

    // networking
    val network = new NetworkModule(authenticator.getAuthenticator, port)
  }
}


// actor infrastructure

class ServerBuilder {
  private val controllers = new HashSet[ControllerRegistration]
  private val actors = new HashSet[ActorRegistration]

  // TODO: unify controller with actor modules, maybe by passing toHub as an explicit parameter to the other actors
  private val controllerQueue = new MessageQueue[Any]("toHub")

  def toHub: MessageSender[Any] = controllerQueue

  def registerController(name: String, controller: Controller) {
    controllers.add(new ControllerRegistration(name,
      new StubProvider(controller)))
  }

  def registerActor(name: String, actor: ActorRunnable) {
    actors.add(new ActorRegistration(name,
      new StubProvider(null: Context),
      new StubProvider(actor)))
  }

  def build(): ActorStarter = {

    // controllers
    val hub = new ControllerHub
    registerActor("Controller", new ActorMessageLoop(hub, controllerQueue))
    ControllerModule.registerControllers(hub, controllers)

    // start up actors
    new ActorStarter(actors)
  }

  private class StubProvider[T](value: T) extends Provider[T] {
    def get(): T = value
  }
}

object ActorModule2 {
  val serverBuilder = new ThreadLocal[ServerBuilder]()
}

abstract class ActorModule2[T] {
  private val builder = ActorModule2.serverBuilder.get
  private val name = getClass.getSimpleName.replace("Module", "")

  private val actorQueue = new MessageQueue[T]("to" + name)

  protected def toHub = builder.toHub

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

class AuthenticatorModule(credentialsChecker: CredentialsChecker[Credentials]) extends ActorModule2[AuthenticatorMessage] {
  private val controller = new AuthenticatorController(toActor)
  private val actor = new AuthenticatorActor(toHub, credentialsChecker)

  registerController(controller)
  registerActor(actor)

  def getAuthenticator: Authenticator = controller
}

class NetworkModule(authenticator: Authenticator, port: Int) extends ActorModule2[NetworkMessage] {
  private val controller = new NetworkController(toActor, authenticator)
  private val actor = new NetworkActor(port, toHub)

  registerController(controller)
  registerActor(actor)
}
