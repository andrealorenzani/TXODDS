package name.lorenzani.andrea.txodds.aktors.protocols

import java.net.InetSocketAddress

import akka.actor.ActorRef
import akka.util.ByteString
import name.lorenzani.andrea.txodds.aktors.InternalStatus
import org.joda.time.{DateTime, DateTimeZone}

object Protocols {
  case class ReceivedData(msg: String) // TcpConnection -> ConnHandler
  case class KeepAliveRequest() // KeepAlive -> ConnHandler to send keepalive
  case class SomeFailure(msg: String, dt: DateTime = DateTime.now(DateTimeZone.UTC)) // TcpClient -> ConnHandler -> WriterStatus to log errors
  case class RequestCompleted(start: Int, end: Int, dt: DateTime = DateTime.now(DateTimeZone.UTC)) // ConnHandler <- && -> WriterStatus
  case class RequestStatus() // Application -> ConnHandler -> WriterStatus
  case class ClosedConnection(name: String) // TcpConn -> ConnHandler
  case class OpenConnection(address: Option[InetSocketAddress] = None) // ConnHandler <- && App->ConnHandler
  case class ReceivedKeepAlive(req: String) // ReceivedData to RKA ConnHandler -> KeepAlive
  case class StopKeepAlive() // connHandler -> WriterStatus
  case class StartKeepAlive(conn: ActorRef) // connHandler -> WriterStatus
  case class LastKeepAlive(dt: DateTime = DateTime.now(DateTimeZone.UTC)) // connHandler -> WriterStatus
  case class LastReconnect(dt: DateTime = DateTime.now(DateTimeZone.UTC)) // connHandler -> WriterStatus

  case class KeepAliveTimeout(dateTime: DateTime = DateTime.now(DateTimeZone.UTC)) // KeepAlive -> ConnHandler -> TcpClient
  case class KeepAliveMessage(dateTime: DateTime = DateTime.now(DateTimeZone.UTC)) // ConnHandler -> TcpClient
  case class GreetServer(singleByte: ByteString = ByteString("0")) //
  case class SendData(str: String) //
}
