package io.sphere.cloudqueues

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server._
import akka.stream.ActorMaterializer

import scala.concurrent.Future

object StartedServer extends Logging {

  def apply(host: String, port: Int, routes: Route)(implicit system: ActorSystem, materializer: ActorMaterializer): StartedServer = {
    import system.dispatcher

    log.info(s"starting HTTP server on $host:$port")

    val bindingFuture = Http().bindAndHandle(routes, host, port)

    StartedServer(host, port, bindingFuture, system)
  }
}

case class StartedServer(host: String, port: Int, bindingFuture: Future[Http.ServerBinding], system: ActorSystem) extends Logging {
  import system.dispatcher

  def stop(): Future[Unit] = {
    log.info(s"stopping HTTP server on $host:$port")
    bindingFuture flatMap (_.unbind()) andThen { case _ ⇒ system.shutdown() }
  }
}
