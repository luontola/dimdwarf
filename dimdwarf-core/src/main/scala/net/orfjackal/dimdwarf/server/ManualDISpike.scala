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
  private val toHub = new MessageQueue[Any]("toHub")
  private val hub = new ControllerHub
  private val builder = new ServerBuilder()

  def configureServer(port: Int, appModule: Module): ActorStarter = {

    // interface with the application using Guice
    // (individually give access to server objects which the application need to access)
    val appInjector = Guice.createInjector(Stage.PRODUCTION, appModule)
    val credentialsChecker = appInjector.getInstance(classOf[CredentialsChecker[Credentials]])

    // controller hub
    builder.registerActor("Controller", new ActorMessageLoop(hub, toHub))

    // TODO: get actor names using reflection

    // authentication
    val authenticator = new AuthenticatorModule(
      new ActorBuilder(builder, "Authenticator", toHub),
      credentialsChecker)

    // networking
    val network = new NetworkModule(
      new ActorBuilder(builder, "Network", toHub),
      authenticator.authenticator, port)

    builder.build(hub)
  }
}

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

  def build(hub: ControllerHub): ActorStarter = {
    // register controllers
    ControllerModule.registerControllers(hub, controllers)

    // start up actors
    new ActorStarter(actors)
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

class AuthenticatorModule(
        builder: ActorBuilder[AuthenticatorMessage],
        credentialsChecker: CredentialsChecker[Credentials]) {
  private val controller = new AuthenticatorController(builder.toActor)
  builder.registerController(controller)
  builder.registerActor(new AuthenticatorActor(builder.toHub, credentialsChecker))

  def authenticator: Authenticator = controller
}

class NetworkModule(
        builder: ActorBuilder[NetworkMessage],
        authenticator: Authenticator,
        port: Int) {
  builder.registerController(new NetworkController(builder.toActor, authenticator))
  builder.registerActor(new NetworkActor(port, builder.toHub))
}

class StubProvider[T](value: T) extends Provider[T] {
  def get(): T = value
}
