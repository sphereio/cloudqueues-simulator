package io.sphere.cloudqueues

import akka.actor.{ActorSystem, Props}
import akka.http.model.HttpHeader
import akka.http.model.StatusCodes._
import akka.http.model.headers.CustomHeader
import akka.http.server.AuthorizationFailedRejection
import akka.http.testkit.ScalatestRouteTest
import io.sphere.cloudqueues.crypto.DefaultSigner
import io.sphere.cloudqueues.oauth.OAuth
import io.sphere.cloudqueues.oauth.OAuth.OAuthToken
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FreeSpec, Matchers}

class QueueTest extends FreeSpec with Matchers with MockitoSugar with ScalatestRouteTest {

  def withActorSystem(testCode: ActorSystem ⇒ Any) = {
    val system = ActorSystem(this.getClass.getSimpleName)
    try testCode(system)
    finally system.shutdown()
  }

  class QueueSetup(system: ActorSystem) {
    val queueManager = system.actorOf(Props[QueueManager])
    val queueInterface = new QueueInterface(queueManager)
    val oauth = new OAuth("secret".getBytes, DefaultSigner)
    val queue = Routes.Queue(queueInterface, oauth)
    val route = queue.route

    def `X-Auth-Token`(token: OAuthToken): HttpHeader = `X-Auth-Token`(token.token)
    def `X-Auth-Token`(token: String): HttpHeader = new CustomHeader {
      override def name(): String = "X-Auth-Token"
      override def value(): String = token
    }
  }

  "creating a queue" - {
    "be rejected without a valid OAuth token" in withActorSystem(new QueueSetup(_) {
      Put("/v1/queues/test").withHeaders(`X-Auth-Token`("hello")) ~> route ~> check {
        rejection shouldEqual AuthorizationFailedRejection
      }
    })

    "is only possible with a OAuth token" in withActorSystem(new QueueSetup(_) {
      val token = oauth.createOAuthToken()
      Put("/v1/queues/test").withHeaders(`X-Auth-Token`(token)) ~> route ~> check {
        status shouldEqual Created
      }
    })
  }

}
