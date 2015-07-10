package io.sphere.cloudqueues

object Main extends App with Logging {

  val startedServer = CloudQueueSimualor.start()

  Runtime.getRuntime.addShutdownHook(new Thread {
    override def run(): Unit = {
      println("bye bye")
      startedServer.stop()
    }
  })

  val lock = new AnyRef
  lock.synchronized { lock.wait() }

}
