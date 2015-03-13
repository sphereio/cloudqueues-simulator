package io.sphere.cloudqueues

import akka.actor.ActorSystem
import akka.http.Http
import akka.http.server._
import akka.stream.ActorFlowMaterializer
import akka.stream.scaladsl.Sink

import scala.concurrent.Future

object StartedServer extends Logging {

  def apply(host: String, port: Int, routes: Route)(implicit system: ActorSystem, materializer: ActorFlowMaterializer): StartedServer = {
    import system.dispatcher

    log.info(s"starting HTTP server on $host:$port")

    val server = Http().bind(host, port)
    val bindingFuture = server.to(Sink.foreach { conn ⇒
      conn.flow.join(routes).run()
    }).run()

    StartedServer(bindingFuture, system)
  }
}

case class StartedServer(bindingFuture: Future[Http.ServerBinding], system: ActorSystem) {
  import system.dispatcher

  def stop(): Future[Unit] =
    bindingFuture flatMap(_.unbind()) andThen { case _ ⇒ system.shutdown() }
}
