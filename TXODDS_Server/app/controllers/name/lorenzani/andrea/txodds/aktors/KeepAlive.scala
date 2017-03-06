package controllers.name.lorenzani.andrea.txodds.aktors

import akka.actor.{Actor, ActorRef, ActorSystem, Cancellable, Props}

object KeepAlive {
  def props = Props[KeepAlive]
}

class KeepAlive extends Actor {
  import protocols.Protocols._
  import context.system
  import play.api.libs.concurrent.Execution.Implicits.defaultContext

  var keepAliveReceived: Option[Cancellable] = None
  var sendKeepAlive: Option[Cancellable] = None
  var tcpConnection: Option[ActorRef] = None

  def receive = {
    case ReceivedKeepAlive(req) =>
      tcpConnection.foreach(conn => resetKeepAliveReceived(conn))
    case StopKeepAlive() =>
      keepAliveReceived.map(_.cancel())
      sendKeepAlive.map(_.cancel())
      context stop self
    case StartKeepAlive(ref) =>
      tcpConnection = Some(ref)
      resetKeepAliveReceived(ref)
      resetSendKeepAlive(ref)
    case x => sender() ! x.getClass.getCanonicalName
  }

  private def resetKeepAliveReceived(sender: ActorRef) = {
    import scala.concurrent.duration._
    keepAliveReceived.map(_.cancel())
    keepAliveReceived = Some(system.scheduler.scheduleOnce(10.seconds, sender, KeepAliveTimeout()))
  }

  private def resetSendKeepAlive(sender: ActorRef) = {
    import scala.concurrent.duration._
    sendKeepAlive.map(_.cancel())
    sendKeepAlive = Some(system.scheduler.scheduleOnce(3.seconds, sender, KeepAliveRequest(sender.path.name)))
  }
}