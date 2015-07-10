package io.sphere.cloudqueues

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class SimpleTest extends Simulation {

  var startedServer: StartedServer = _

  before {
    startedServer = CloudQueueSimualor.start(Some("localhost"), Some(30002))
  }

  after {
    startedServer.stop()
  }

  val httpConf = http
     .baseURL(s"http://localhost:30002")
     .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
     .acceptLanguageHeader("en-US,en;q=0.5")
     .acceptEncodingHeader("gzip, deflate")
     .userAgentHeader("Mozilla/5.0 (Windows NT 5.1; rv:31.0) Gecko/20100101 Firefox/31.0")

  val scn = scenario("BasicSimulation")
     .exec(http("request_1")
     .get("/"))
     .pause(1)

  setUp(scn.inject(rampUsers(100).over(1 second))).protocols(httpConf)
}
