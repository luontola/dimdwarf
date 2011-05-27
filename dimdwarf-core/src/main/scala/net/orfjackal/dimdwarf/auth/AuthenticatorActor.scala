package net.orfjackal.dimdwarf.auth

import net.orfjackal.dimdwarf.mq.MessageSender
import net.orfjackal.dimdwarf.controller.Hub
import net.orfjackal.dimdwarf.actors._
import javax.inject.Inject

@ActorScoped
class AuthenticatorActor @Inject()(@Hub toHub: MessageSender[Any], checker: CredentialsChecker[Credentials]) extends Actor[AuthenticatorMessage] {
  def start() {}

  def process(message: AuthenticatorMessage) {
    // TODO: consider avoiding @unchecked by having different base classes for incoming and outgoing messages (if distinct sets)
    (message: @unchecked) match {
      case IsUserAuthenticated(credentials) =>
        if (checker.isValid(credentials)) {
          toHub.send(YesUserIsAuthenticated(credentials))
        } else {
          toHub.send(NoUserIsNotAuthenticated(credentials))
        }
    }
  }
}
