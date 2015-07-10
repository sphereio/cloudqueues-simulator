package io.sphere.cloudqueues

import akka.actor.{Props, ActorSystem}
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorFlowMaterializer
import com.github.kxbmap.configs._
import com.typesafe.config.ConfigFactory
import io.sphere.cloudqueues.crypto.DefaultSigner
import io.sphere.cloudqueues.oauth.OAuth

object CloudQueueSimualor extends Logging {

  def start(host: Option[String] = None, port: Option[Int] = None): StartedServer = {
    val conf = ConfigFactory.load()
    implicit val system = ActorSystem("cloudqueues", conf)
    import system.dispatcher
    implicit val materializer = ActorFlowMaterializer()

    val httpHost = host getOrElse "0.0.0.0"
    val httpPort = port getOrElse conf.get[Int]("http.port")
    val secretKey = conf.get[String]("secret").getBytes("UTF-8")

    val queueManager = system.actorOf(Props[QueueManager])
    val queueInterface = new QueueInterface(queueManager)

    val oauth = new OAuth(secretKey, DefaultSigner)
    val auth = Routes.Auth(oauth)
    val queue = Routes.Queue(queueInterface, oauth)

    val routes = Routes.index ~ auth.route ~ queue.route
    val startedServer = StartedServer(httpHost, httpPort, routes)
    log.info(s"cloud queues simulation started on port $httpPort")
    startedServer
  }

}
