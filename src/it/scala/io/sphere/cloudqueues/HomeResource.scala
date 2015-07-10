package io.sphere.cloudqueues

import io.gatling.core.Predef._
import io.gatling.http.Predef._


object HomeResource {

  val homeResource = exec(http("home")
    .get("/")
    .check(status.is(200))
    .check(bodyString.is("cloud queues simulator")))
    .pause(1)
}
