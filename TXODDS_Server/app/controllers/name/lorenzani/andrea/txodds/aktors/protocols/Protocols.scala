package controllers.name.lorenzani.andrea.txodds.aktors.protocols

import java.net.InetSocketAddress

import akka.actor.ActorRef
import akka.util.ByteString
import org.joda.time.{DateTime, DateTimeZone}

object Protocols {
  case class ReceivedData(msg: String)
  case class KeepAliveRequest(name: String)
  case class SomeFailure(msg: String, dt: DateTime = DateTime.now(DateTimeZone.UTC))
  case class RequestStatus()
  case class ClosedConnection()
  case class OpenConnection(address: Option[InetSocketAddress] = None)
  case class ReceivedKeepAlive(req: String)
  case class StopKeepAlive()
  case class StartKeepAlive(ref: ActorRef)
  case class KeepAliveTimeout(dateTime: DateTime = DateTime.now(DateTimeZone.UTC))
  case class KeepAliveMessage(dateTime: DateTime = DateTime.now(DateTimeZone.UTC))
  case class SendData(str: String)
  case class ConnectionEstabilished(tcp: ActorRef, keepAlive: ActorRef)
}
