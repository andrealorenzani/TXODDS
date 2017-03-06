package name.lorenzani.andrea.txodds.aktors

import akka.actor.{Actor, ActorLogging, ActorRef, Cancellable, Props}
import org.joda.time.{DateTime, DateTimeZone}

import scala.collection.mutable.ListBuffer

object WriterStatus {
  def props = Props(classOf[WriterStatus])
}

class WriterStatus extends Actor with ActorLogging {
  import protocols.Protocols._

  var nerrors = 0
  var connStatus = HealthStatus.Red
  var requestsCompleted = new ListBuffer[RequestCompleted]()
  var errors = new ListBuffer[SomeFailure]()
  var lastReconnect: Option[DateTime] = None
  var lastKeepAlive: Option[DateTime] = None

  def receive = {
    case x: SomeFailure =>
      nerrors += 1
      errors += x
      errors = errors.takeRight(100)
    case request: RequestCompleted =>
      requestsCompleted += request
      requestsCompleted = requestsCompleted.takeRight(1000)
    case statusReq: RequestStatus =>
      log.info("Request status reply")
      sender() ! InternalStatus(nerrors, connStatus, lastReconnect, lastKeepAlive, requestsCompleted.toList, errors.toList.map(x => s"${x.dt.toString("HH:mm:ss")} - ${x.msg}"))
    case status: HealthStatus.Value =>
      connStatus = status
    case LastReconnect(dt) => lastReconnect = Some(dt)
    case LastKeepAlive(dt) => lastKeepAlive = Some(dt)
    case x => sender() ! InternalStatus(nerrors, connStatus, lastReconnect, lastKeepAlive, requestsCompleted.toList, errors.toList.map(x => s"${x.dt.toString("HH:mm:ss")} - ${x.msg}"))
  }
}