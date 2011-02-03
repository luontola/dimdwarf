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
  private val actors = new HashSet[ActorRegistration]
  private val controllers = new HashSet[ControllerRegistration]

  def configureServer(port: Int, appModule: Module): ActorStarter = {

    // interface with the application using Guice
    // (individually give access to server objects which the application need to access)
    val appInjector = Guice.createInjector(Stage.PRODUCTION, appModule)
    val credentialsChecker = appInjector.getInstance(classOf[CredentialsChecker[Credentials]])

    // controller hub
    val toHub = new MessageQueue[Any]("toHub")
    val hub = new ControllerHub
    registerActor("Controller", new ActorMessageLoop(hub, toHub))

    // authentication
    val authenticator = {
      val name = "Authenticator"
      val toActor = new MessageQueue[AuthenticatorMessage]("to" + name)

      val module = new AuthenticatorModule(toActor, toHub, credentialsChecker)

      registerController(name, module.controller)
      registerActor(name, new ActorMessageLoop(module.actor, toActor))

      module.authenticator
    }

    // networking
    {
      val name = "Network"
      val toActor = new MessageQueue[NetworkMessage]("to" + name)

      val module = new NetworkModule(toActor, toHub, authenticator, port)

      registerController(name, module.controller)
      registerActor(name, new ActorMessageLoop(module.actor, toActor))
    }

    // register controllers
    ControllerModule.registerControllers(hub, controllers)

    // start up actors
    new ActorStarter(actors)
  }

  def registerController(name: String, controller: Controller) {
    controllers.add(new ControllerRegistration(name,
      new StubProvider(controller)))
  }

  def registerActor(name: String, actor: ActorRunnable) {
    actors.add(new ActorRegistration(name,
      new StubProvider(null: Context),
      new StubProvider(actor)))
  }
}

class AuthenticatorModule(
        toActor: MessageSender[AuthenticatorMessage],
        toHub: MessageSender[Any],
        credentialsChecker: CredentialsChecker[Credentials]) {
  val controller = new AuthenticatorController(toActor)
  val actor = new AuthenticatorActor(toHub, credentialsChecker)

  def authenticator: Authenticator = controller
}

class NetworkModule(
        toActor: MessageSender[NetworkMessage],
        toHub: MessageSender[Any],
        authenticator: Authenticator,
        port: Int) {
  val controller = new NetworkController(toActor, authenticator)
  val actor = new NetworkActor(port, toHub)
}

class StubProvider[T](value: T) extends Provider[T] {
  def get(): T = value
}
