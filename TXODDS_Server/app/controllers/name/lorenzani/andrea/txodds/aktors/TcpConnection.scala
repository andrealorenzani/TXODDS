package controllers.name.lorenzani.andrea.txodds.aktors

import akka.actor.{Actor, ActorRef, Props}
import akka.util.ByteString
import controllers.name.lorenzani.andrea.txodds.aktors.protocols.Protocols._

import scala.util.Try

object TcpConnection {
  def props(conn: ActorRef, listener: ActorRef, ka:ActorRef) = Props(classOf[TcpConnection], conn, listener, ka)
}

class TcpConnection(connection: ActorRef,
                        listener: ActorRef,
                        keepAlive: ActorRef) extends Actor {
  import akka.io.Tcp._


  val me = self.path.name
  def receive = {
    case KeepAliveMessage(dt) =>
      connection ! Write(ByteString(s"$me,${dt.getMillis}"))
    case Received(data) =>
      if(Try{data.toString().split(",")(0).toInt}.isSuccess) {
        listener ! ReceivedData(data.toString())
      }
      else {
        keepAlive ! ReceivedKeepAlive(data.toString())
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
    case x: ConnectionClosed =>
      listener ! SomeFailure("connection closed")
      listener ! x
      keepAlive ! StopKeepAlive()
      context stop self
  }
}
