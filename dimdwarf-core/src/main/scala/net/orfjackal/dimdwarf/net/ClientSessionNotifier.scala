package net.orfjackal.dimdwarf.net

trait ClientSessionNotifier {
  def fireLoginSuccess(session: SessionHandle): Unit

  def fireLoginFailure(session: SessionHandle): Unit

  def fireLogoutSuccess(session: SessionHandle): Unit
}
