package io.sphere.cloudqueues.oauth

import akka.actor.ActorSystem
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import io.sphere.cloudqueues.Routes
import io.sphere.cloudqueues.crypto.DefaultSigner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FreeSpec, Matchers}

class OAuthRouteTest extends FreeSpec with Matchers with MockitoSugar with ScalatestRouteTest {

  def withActorSystem(testCode: ActorSystem ⇒ Any) = {
    val system = ActorSystem(this.getClass.getSimpleName)
    try testCode(system)
    finally system.shutdown()
  }

  class QueueSetup(system: ActorSystem) {
    val oauth = new OAuth("secret".getBytes, DefaultSigner)
    val route = Routes.Auth(oauth).route
  }

  "POST /v2.0/tokens" - {
    "should deliver an OAuth token 2" in withActorSystem(new QueueSetup(_) {
      val payload =
        """{
          |  "auth:{
          |    "RAX-KSKEY:apiKeyCredentials":{
          |      "username": "not-checked-yet",
          |      "apiKey": "not-checked-yet"
          |    }
          |  }
          |}
        """.stripMargin
      Post("/v2.0/tokens", HttpEntity(`application/json`, payload)) ~> route ~> check {
        status shouldEqual OK
      }
    })
  }

}
