package io.sphere.cloudqueues

import akka.actor.{Props, ActorSystem}
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorFlowMaterializer
import com.github.kxbmap.configs._
import com.typesafe.config.ConfigFactory
import io.sphere.cloudqueues.crypto.DefaultSigner
import io.sphere.cloudqueues.oauth.OAuth

object Main extends App with Logging {

  val conf = ConfigFactory.load()
  implicit val system = ActorSystem("cloudqueues", conf)
  import system.dispatcher
  implicit val materializer = ActorFlowMaterializer()

  val httpPort = conf.get[Int]("http.port")
  val secretKey = conf.get[String]("secret").getBytes("UTF-8")

  val queueManager = system.actorOf(Props[QueueManager])
  val queueInterface = new QueueInterface(queueManager)

  val oauth = new OAuth(secretKey, DefaultSigner)
  val auth = Routes.Auth(oauth)
  val queue = Routes.Queue(queueInterface, oauth)

  val routes = Routes.index ~ auth.route ~ queue.route
  val startedServer = StartedServer("0.0.0.0", httpPort, routes)


  Runtime.getRuntime.addShutdownHook(new Thread {
    override def run(): Unit = {
      println("bye bye")
      startedServer.stop()
    }
  })

  log.info(s"cloud queues simulation started on port $httpPort")
  val lock = new AnyRef
  lock.synchronized { lock.wait() }

}
