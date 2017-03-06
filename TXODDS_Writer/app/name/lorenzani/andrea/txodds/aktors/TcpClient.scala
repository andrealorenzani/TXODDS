package name.lorenzani.andrea.txodds.aktors

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorRef, Cancellable, Props}
import akka.io.{IO, Tcp}
import akka.util.ByteString

import scala.util.Try

object TcpClient {
  def props(remote: InetSocketAddress, handler: ActorRef, name: String) =
    Props(classOf[TcpClient], remote, handler, name)


}

class TcpClient(remote: InetSocketAddress, listener: ActorRef, name: String) extends Actor {

  import Tcp._
  import context.system
  import protocols.Protocols._
  import play.api.libs.concurrent.Execution.Implicits.defaultContext

  var keepAlive: Option[ActorRef] = None
  var retryMechanism: Option[Cancellable] = None

  IO(Tcp) ! Connect(remote)

  def receive = {
    case CommandFailed(_: Connect) =>
      import scala.concurrent.duration._
      listener ! SomeFailure("connect failed")
      listener ! ClosedConnection(name)
      if(retryMechanism.isEmpty) {
        retryMechanism = Some(context.system.scheduler.schedule(5.seconds, 10.seconds, IO(Tcp), Connect(remote)))
      }

    case c@Connected(remote, local) =>
      listener ! c
      val connection = sender()
      connection ! Register(self)
      retryMechanism = None
      self ! GreetServer(ByteString("0"))
      context become {
        case StartKeepAlive(ka) =>
          keepAlive = Some(ka)
        case GreetServer(greet) =>
          connection ! Write(greet)
        case KeepAliveMessage(dt) =>
          connection ! Write(ByteString(s"$name,${dt.getMillis}"))
        case Received(data) =>
          if (Try { data.toString().split(",")(0).toInt }.isFailure) {
            keepAlive.foreach(_ ! ReceivedKeepAlive(data.toString()))
          } else {
            listener ! ReceivedData(data.toString())
          }
        case KeepAliveTimeout(dt) =>
          connection ! Close
        // Those are "standard" methods
        case SendData(data) =>
          connection ! Write(ByteString(data))
        case data: ByteString =>
          connection ! Write(data)
        case CommandFailed(w: Write) =>
          // O/S buffer was full
          listener ! SomeFailure("write failed")
        case _: ConnectionClosed =>
          listener ! SomeFailure("connection closed")
          listener ! ClosedConnection(name)
          context.unbecome()
          IO(Tcp) ! Connect(remote)
      }
  }
}