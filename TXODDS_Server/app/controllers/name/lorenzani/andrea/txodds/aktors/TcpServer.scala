package controllers.name.lorenzani.andrea.txodds.aktors

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorRef, Cancellable, Props}
import akka.io.{IO, Tcp}
import akka.io.Tcp._
import akka.util.ByteString
import controllers.name.lorenzani.andrea.txodds.aktors.protocols.Protocols._

import scala.util.Try

object TcpServer {
  def props(address: InetSocketAddress, listener: ActorRef) = Props(classOf[TcpServer], address, listener)
}

class TcpServer(address: InetSocketAddress, listener: ActorRef) extends Actor {

  import Tcp._
  import context.system
  import play.api.libs.concurrent.Execution.Implicits.defaultContext

  val me = self.path.name
  var retryBound: Option[Cancellable] = None

  IO(Tcp) ! Bind(self, address)

  def receive = {
    case b @ Bound(localAddress) =>
      retryBound.map(_.cancel())
      listener ! b

    case CommandFailed(_: Bind) =>
      listener ! SomeFailure("not bound")
      rebound

    case c @ Connected(remote, local) =>
      val connection = sender()
      val keepAlive = system.actorOf(KeepAlive.props)
      val tcp = system.actorOf(TcpConnection.props(connection, listener, keepAlive))
      connection ! Register(tcp)
      keepAlive ! StartKeepAlive(tcp)
      listener ! ConnectionEstabilished(tcp, keepAlive)
  }

  private def rebound = {
    import scala.concurrent.duration._
    if(retryBound.isEmpty) {
      retryBound = Some(system.scheduler.schedule(5.seconds, 10.seconds, IO(Tcp), Bind(self, address)))
    }
  }

}
