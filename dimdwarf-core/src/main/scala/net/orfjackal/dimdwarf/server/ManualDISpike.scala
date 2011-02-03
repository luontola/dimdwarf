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

object ManualDISpike {
  def configureServer(port: Int, appModule: Module): ActorStarter = {
    val actors = new HashSet[ActorRegistration]
    val controllers = new HashSet[ControllerRegistration]

    // interface with the application using Guice
    // (individually give access to server objects which the application need to access)
    val appInjector = Guice.createInjector(Stage.PRODUCTION, appModule)
    val credentialsChecker = appInjector.getInstance(classOf[CredentialsChecker[Credentials]])

    // controller hub
    val toHub = new MessageQueue[Any]("toHub")
    val hub = new ControllerHub
    actors.add(new ActorRegistration("Controller",
      new StubProvider(new ControllerContext(null)),
      new StubProvider(new ActorMessageLoop(hub, toHub))))

    // authentication
    val toAuthenticator = new MessageQueue[AuthenticatorMessage]("toAuthenticator")

    val authenticator = new AuthenticatorModule(toAuthenticator, toHub, credentialsChecker)

    controllers.add(new ControllerRegistration("Authenticator", new StubProvider(authenticator.controller)))
    actors.add(new ActorRegistration("Authenticator",
      new StubProvider(new ActorContext(null)),
      new StubProvider(new ActorMessageLoop(authenticator.actor, toAuthenticator))))

    // networking
    val toNetwork = new MessageQueue[NetworkMessage]("toNetwork")

    val network = new NetworkModule(toNetwork, toHub, authenticator.authenticator, port)

    controllers.add(new ControllerRegistration("Network", new StubProvider(network.controller)))
    actors.add(new ActorRegistration("Network",
      new StubProvider(new ActorContext(null)),
      new StubProvider(new ActorMessageLoop(network.actor, toNetwork))))

    // register controllers
    ControllerModule.registerControllers(hub, controllers)

    // start up actors
    new ActorStarter(actors)
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
