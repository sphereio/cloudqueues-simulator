package io.sphere.cloudqueues

import akka.actor.{Props, ActorSystem}
import akka.http.server.Directives._
import akka.stream.ActorFlowMaterializer
import com.github.kxbmap.configs._
import com.typesafe.config.ConfigFactory

object Main extends App with Logging {

  val conf = ConfigFactory.load()
  implicit val system = ActorSystem("cloudqueues", conf)
  import system.dispatcher
  implicit val materializer = ActorFlowMaterializer()

  val httpPort = conf.get[Int]("http.port")
  val secretKey = conf.get[String]("secret").getBytes("UTF-8")

  val queueManager = system.actorOf(Props[QueueManager])
  val queueInterface = new QueueInterface(queueManager)

  val auth = Routes.Auth(secretKey)
  val queue = Routes.Queue(queueInterface)

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
