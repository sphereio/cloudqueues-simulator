package io.sphere.cloudqueues

import io.gatling.core.Predef._
import io.gatling.http.Predef._

object QueueResource {

  def createQueue(name: String) =
    OAuthTokenResource.oauthToken
    .exec(http("createqueue")
      .put(s"/v1/queues/$name")
      .header("X-Auth-Token", "${token}")
      .check(status.in(201, 204)))
}
