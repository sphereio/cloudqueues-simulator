package io.sphere.cloudqueues

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class PerfTests extends Simulation {

  var startedServer: StartedServer = _

  before {
    startedServer = CloudQueueSimualor.start(Some("localhost"), Some(30002))
  }

  after {
    startedServer.stop()
  }

  val httpConf = http
     .baseURL(s"http://localhost:30002")
     .acceptEncodingHeader("gzip, deflate")

  val users = scenario("users")
    .exec(
      HomeResource.homeResource,
      OAuthTokenResource.oauthToken,
      QueueResource.createQueue("test"))

  setUp(users.inject(atOnceUsers(10))).protocols(httpConf)
}
