package name.lorenzani.andrea.txodds.aktors

import javax.inject.Inject

import akka.actor.{Actor, ActorRef, ActorSystem, Cancellable, Props}
import play.api.libs.concurrent.Execution.Implicits.defaultContext

object KeepAlive {
  def props = Props[KeepAlive]
}

class KeepAlive() extends Actor {
  import protocols.Protocols._

  var keepAliveReceived: Option[Cancellable] = None
  var sendKeepAlive: Option[Cancellable] = None

  def receive = {
    case ReceivedKeepAlive(req) =>
      resetKeepAliveReceived(sender())
    case StopKeepAlive() =>
      keepAliveReceived.map(_.cancel())
      sendKeepAlive.map(_.cancel())
    case StartKeepAlive(conn) =>
      conn ! StartKeepAlive(self)
      resetKeepAliveReceived(conn)
      resetSendKeepAlive(conn)
    case x => sender() ! x.getClass.getCanonicalName
  }

  private def resetKeepAliveReceived(sender: ActorRef) = {
    import scala.concurrent.duration._
    keepAliveReceived.map(_.cancel())
    keepAliveReceived = Some(context.system.scheduler.scheduleOnce(10.seconds, sender, KeepAliveTimeout()))
  }

  private def resetSendKeepAlive(sender: ActorRef) = {
    import scala.concurrent.duration._
    sendKeepAlive.map(_.cancel())
    sendKeepAlive = Some(context.system.scheduler.schedule(1.second, 3.seconds, sender, KeepAliveRequest()))
  }
}