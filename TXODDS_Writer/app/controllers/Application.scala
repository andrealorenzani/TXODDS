package controllers

import java.net.InetSocketAddress

import akka.actor.ActorSystem
import play.api._
import play.api.mvc._
import javax.inject._

import akka.pattern.ask
import akka.util.Timeout
import name.lorenzani.andrea.txodds.aktors.protocols.Protocols.{OpenConnection, RequestStatus}
import name.lorenzani.andrea.txodds.aktors.{ConnectionsHandler, InternalStatus, TcpClient}

import scala.concurrent.Await
import scala.concurrent.duration._

// https://www.playframework.com/documentation/2.5.x/ScalaAkka

@Singleton
class Application @Inject() (system: ActorSystem, configuration: play.api.Configuration) extends Controller {

  val connectionHandler = system.actorOf(ConnectionsHandler.props(system, configuration), "conn-handler-actor")
  connectionHandler ! OpenConnection()
  implicit val timeout = Timeout(10 seconds)

  def index = {
    val future = (connectionHandler ? RequestStatus()).mapTo[InternalStatus]
    val result = Await.result(future, timeout.duration)
    Action(Ok(views.html.index(result)))
  }
}
