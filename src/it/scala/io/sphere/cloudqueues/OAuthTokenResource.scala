package io.sphere.cloudqueues

import io.gatling.core.Predef._
import io.gatling.http.Predef._

object OAuthTokenResource {

  val oauthToken = exec(http("home")
    .post("/v2.0/tokens")
    .body(StringBody("{}"))
    .check(status.is(200))
    .check(jsonPath("$.access.token.id").exists.saveAs("token")))

}
