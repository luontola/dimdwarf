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
    val builder = new ServerBuilder()

    // interface with the application using Guice
    // (individually give access to server objects which the application need to access)
    val appInjector = Guice.createInjector(Stage.PRODUCTION, appModule)
    val credentialsChecker = appInjector.getInstance(classOf[CredentialsChecker[Credentials]])

    // TODO: get actor names using reflection

    // authentication
    val authenticator = new AuthenticatorModule(
      new ActorBuilder(builder, "Authenticator", builder.toHub),
      credentialsChecker)

    // networking
    val network = new NetworkModule(
      new ActorBuilder(builder, "Network", builder.toHub),
      authenticator.getAuthenticator, port)

    builder.build()
  }
}


// actor infrastructure

class ServerBuilder {
  private val controllers = new HashSet[ControllerRegistration]
  private val actors = new HashSet[ActorRegistration]

  // TODO: unify controller with actor modules
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

class ActorBuilder[T](builder: ServerBuilder, name: String, val toHub: MessageSender[Any]) {
  private val actorQueue = new MessageQueue[T]("to" + name)

  def toActor: MessageSender[T] = actorQueue

  def registerController(controller: Controller) {
    builder.registerController(name, controller)
  }

  def registerActor(actor: Actor[T]) {
    registerActorRunnable(new ActorMessageLoop(actor, actorQueue))
  }

  def registerActorRunnable(actor: ActorRunnable) {
    builder.registerActor(name, actor)
  }
}


// individual actor modules

class AuthenticatorModule(builder: ActorBuilder[AuthenticatorMessage],
                          credentialsChecker: CredentialsChecker[Credentials]) {
  private val controller = new AuthenticatorController(builder.toActor)
  private val actor = new AuthenticatorActor(builder.toHub, credentialsChecker)

  builder.registerController(controller)
  builder.registerActor(actor)

  def getAuthenticator: Authenticator = controller
}

class NetworkModule(builder: ActorBuilder[NetworkMessage],
                    authenticator: Authenticator,
                    port: Int) {
  private val controller = new NetworkController(builder.toActor, authenticator)
  private val actor = new NetworkActor(port, builder.toHub)

  builder.registerController(controller)
  builder.registerActor(actor)
}
