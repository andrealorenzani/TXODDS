package controllers

import java.net.{InetAddress, InetSocketAddress}
import javax.inject.{Inject, Singleton}

import akka.actor.ActorSystem
import play.api._
import play.api.mvc._
import name.lorenzani.andrea.txodds.aktors.WriterHandler
import name.lorenzani.andrea.txodds.aktors.TcpServer

@Singleton
class Application @Inject() (system: ActorSystem,
                             configuration: play.api.Configuration) extends Controller {
  val writeHandler = system.actorOf(WriterHandler.props)
  val address = new InetSocketAddress(configuration.underlying.getString("server.host"),
                                      configuration.underlying.getInt("server.writer.port"))
  val writerServer = system.actorOf(TcpServer.props(address, writeHandler))

  def index = Action {
    Ok(views.html.index("This is a test to see how to contribute a template"))
  }

}
