package name.lorenzani.andrea.txodds.aktors

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Cancellable, Props}
import akka.io.Tcp.Connected
import akka.util.Timeout
import akka.pattern.ask
import org.joda.time.DateTime
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import name.lorenzani.andrea.txodds.aktors.KeepAlive

import scala.concurrent.Await
import scala.util.Try

object HealthStatus extends Enumeration {
  type status = Value
  val Red, Yellow, Green = Value
}

case class InternalStatus(errors: Int,
                          status: HealthStatus.Value,
                          conStarted: Option[DateTime],
                          lastKA: Option[DateTime],
                          lastReq: List[protocols.Protocols.RequestCompleted],
                          lastErr: List[String] = List())

object ConnectionsHandler {
  def props(system: ActorSystem, configuration: play.api.Configuration) =
    Props(classOf[ConnectionsHandler], system, configuration)
}

class ConnectionsHandler(system: ActorSystem, configuration: play.api.Configuration) extends Actor with ActorLogging {
  import protocols.Protocols._

  val address = new InetSocketAddress(configuration.underlying.getString("server.address"),
                                      configuration.underlying.getInt("server.port"))
  val internalStatus = Try{system.actorOf(WriterStatus.props, "internal-actor")}.toOption
  val keepAlive = Try{system.actorOf(KeepAlive.props, "keepalive-actor")}.toOption
  var connection: Option[ActorRef] = None
  var numWriter = 0

  def receive = {
    case ReceivedData(data: String) =>
      log.info(s"Received data $data")
        handleRequest(data)

    case ClosedConnection(name) =>
      import scala.concurrent.duration._
      connection = None
      internalStatus.foreach(_ ! HealthStatus.Red)
      internalStatus.foreach(_ ! SomeFailure("Closed Connection"))
      keepAlive.foreach(_ ! StopKeepAlive())
    case OpenConnection(conn) =>
      internalStatus.foreach(_ ! HealthStatus.Yellow)
      numWriter += 1
      if(connection.isEmpty){
        connection = Some(system.actorOf(TcpClient.props(conn.getOrElse(address), self, s"Writer-$numWriter"), "writer-actor"))
      }
    case Connected(remoteAddr, localAddr) =>
      internalStatus.foreach(_ ! HealthStatus.Green)
      internalStatus.foreach(_ ! LastReconnect())
      for{ka <- keepAlive
          conn <- connection}{
        ka ! StartKeepAlive(conn)
      }
    case x: SomeFailure =>
      internalStatus.foreach(_ ! x)
      log.error(s"Failure: ${x.msg}")
    case rc: RequestCompleted =>
      log.info(s"Request completed [${rc.start}, ${rc.end})")
      internalStatus.foreach(_ ! rc)
    case RequestStatus() =>
      log.info("Request status to internal status")
      import scala.concurrent.duration._
      implicit val timeout = Timeout(5 seconds)
      internalStatus.foreach { status: ActorRef =>
        val res = (status ? RequestStatus()).mapTo[InternalStatus]
        sender() ! Await.result(res, timeout.duration)
      }
    case x => sender() ! InternalStatus(0, HealthStatus.Red, None, None, List(), List(x.getClass.getCanonicalName))
  }

  private def handleRequest(data: String) = {
    val recReq = data.split(",").map(_.toInt)
    if(recReq.length != 2) self ! SomeFailure(s"Received request with bad format: $data")
    if(recReq(1) == 0) self ! SomeFailure("Received request with 0 numbers")
    else {
      val (start, end) = if (recReq(1) >= 0) {
        (recReq(0), recReq(0)+recReq(1))
      } else {
        (recReq(0)+recReq(1), recReq(0))
      }
      connection.foreach(_ ! SendData((start until end).map(_.toString).mkString(",")))
      self ! RequestCompleted(start, end)
    }
  }

}
