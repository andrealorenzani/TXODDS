package controllers.name.lorenzani.andrea.txodds.aktors

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.actor.Actor.Receive
import akka.io.Tcp.ConnectionClosed
import controllers.name.lorenzani.andrea.txodds.aktors.protocols.Protocols._

object WriterHandler {
  def props = Props[WriterHandler]
}

class WriterHandler extends Actor with ActorLogging {
  var writer: Option[ConnectionEstabilished] = None
  var errors = 0
  var lastFailure: Option[SomeFailure] = None

  override def receive = {
    case x: ConnectionEstabilished =>
      writer = Some(x)
    case x: ConnectionClosed =>
      writer = None
    case f @ SomeFailure(failure, dt) =>
      errors += 1
      log.error(s"${dt.formatted("HH:mm:ss")} - $failure")
      lastFailure = Some(f)
  }
}
