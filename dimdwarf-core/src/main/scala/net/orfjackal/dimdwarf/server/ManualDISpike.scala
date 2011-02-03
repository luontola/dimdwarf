package net.orfjackal.dimdwarf.server

import com.google.inject._
import net.orfjackal.dimdwarf.auth._
import net.orfjackal.dimdwarf.actors._
import java.util.HashSet
import net.orfjackal.dimdwarf.mq.MessageQueue
import net.orfjackal.dimdwarf.net._
import net.orfjackal.dimdwarf.controller._
import net.orfjackal.dimdwarf.modules.ControllerModule
import javax.inject.Provider

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
    val authenticatorController = new AuthenticatorController(toAuthenticator)
    val authenticatorActor = new AuthenticatorActor(toHub, credentialsChecker)
    controllers.add(new ControllerRegistration("Authenticator", new StubProvider(authenticatorController)))
    actors.add(new ActorRegistration("Authenticator",
      new StubProvider(new ActorContext(null)),
      new StubProvider(new ActorMessageLoop(authenticatorActor, toAuthenticator))))

    // networking
    val toNetwork = new MessageQueue[NetworkMessage]("toNetwork")
    val networkController = new NetworkController(toNetwork, authenticatorController)
    val networkActor = new NetworkActor(port, toHub)
    controllers.add(new ControllerRegistration("Network", new StubProvider(networkController)))
    actors.add(new ActorRegistration("Network",
      new StubProvider(new ActorContext(null)),
      new StubProvider(new ActorMessageLoop(networkActor, toNetwork))))

    // register controllers
    ControllerModule.registerControllers(hub, controllers)

    // start up actors
    new ActorStarter(actors)
  }
}

class StubProvider[T](value: T) extends Provider[T] {
  def get(): T = value
}
