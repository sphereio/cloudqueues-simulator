package io.sphere.cloudqueues

import java.util.UUID

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.headers.CustomHeader
import akka.http.scaladsl.model.{HttpEntity, HttpHeader, HttpRequest}
import akka.http.scaladsl.server.AuthorizationFailedRejection
import akka.http.scaladsl.testkit.ScalatestRouteTest
import io.sphere.cloudqueues.crypto.DefaultSigner
import io.sphere.cloudqueues.oauth.OAuth
import io.sphere.cloudqueues.oauth.OAuth.OAuthToken
import io.sphere.cloudqueues.util.FutureAwaits._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{OptionValues, FreeSpec, Matchers}
import spray.json._

class QueueTest extends FreeSpec with Matchers with MockitoSugar with OptionValues with ScalatestRouteTest {

  def withActorSystem(testCode: ActorSystem ⇒ Any) = {
    val system = ActorSystem(this.getClass.getSimpleName)
    try testCode(system)
    finally system.terminate()
  }

  class QueueSetup(system: ActorSystem) {
    val queueManager = system.actorOf(Props[QueueManager])
    val queueInterface = new QueueInterface(queueManager)
    val oauth = new OAuth("secret".getBytes, DefaultSigner)
    val queueRoutes = Routes.Queue(queueInterface, oauth)
    val route = queueRoutes.route

    def `X-Auth-Token`(token: OAuthToken): HttpHeader = `X-Auth-Token`(token.token)
    def `X-Auth-Token`(token: String): HttpHeader = new CustomHeader {
      override def name(): String = "X-Auth-Token"
      override def value(): String = token
    }

    lazy val token = oauth.createOAuthToken()
    val authenticated: HttpRequest ⇒ HttpRequest = req ⇒{
      req.withHeaders(`X-Auth-Token`(token))
    }

    def withQueue[A](block: QueueName ⇒ A) = {
      val queue = QueueName(UUID.randomUUID().toString)
      await(queueInterface.newQueue(queue))
      block(queue)
    }

    def addMessages(queue: QueueName) =
      await(queueInterface.addMessages(queue, Message(20, JsObject("a" → JsString("b"))) :: Nil))
  }

  "creating a queue" - {
    "be rejected without a valid OAuth token" in withActorSystem(new QueueSetup(_) {
      Put("/v1/queues/test").withHeaders(`X-Auth-Token`("hello")) ~> route ~> check {
        rejection shouldEqual AuthorizationFailedRejection
      }
    })

    "is only possible with a OAuth token" in withActorSystem(new QueueSetup(_) {
      Put("/v1/queues/test") ~> authenticated ~> route ~> check {
        status shouldEqual Created
      }
    })
  }

  "adding messages into a queue" - {
    "is not possible when the queue does not exist" in withActorSystem(new QueueSetup(_) {
      Post("/v1/queues/test/messages", HttpEntity(`application/json`, "[]")) ~> authenticated ~> route ~> check {
        status shouldEqual NotFound
      }
    })

    "is possible" in withActorSystem(new QueueSetup(_) {
      withQueue { queue ⇒
        Post(s"/v1/queues/$queue/messages", HttpEntity(`application/json`, "[]")) ~> authenticated ~> route ~> check {
          status shouldEqual Created
        }
      }
    })
  }

  "claiming messages from a queue" - {

    val claim =
      """{
        |  "ttl": 20,
        |  "grace": 23
        |}
      """.stripMargin

    "is not possible when the queue does not exist" in withActorSystem(new QueueSetup(_) {
      Post("/v1/queues/test/claims", HttpEntity(`application/json`, claim)) ~> authenticated ~> route ~> check {
        status shouldEqual NotFound
      }
    })

    "is possible if the queue does not contain any messages" in withActorSystem(new QueueSetup(_) {
      withQueue { queue ⇒
        Post(s"/v1/queues/$queue/claims", HttpEntity(`application/json`, claim)) ~> authenticated ~> route ~> check {
          status shouldEqual NoContent
        }
      }
    })

    "is possible if the queue contains messages" in withActorSystem(new QueueSetup(_) {
      withQueue { queue ⇒
        val message = addMessages(queue).value
        val msgId = message.messages.head.id
        Post(s"/v1/queues/$queue/claims", HttpEntity(`application/json`, claim)) ~> authenticated ~> route ~> check {
          status shouldEqual Created
          val responseAsString = responseAs[String]
          withClue(responseAsString) {
            val json = responseAsString.parseJson
            json shouldBe a [JsArray]
            val first = json.asInstanceOf[JsArray].elements.head
            first shouldBe a [JsObject]
            val claim = first.asJsObject.fields.get("href")
            val url = s"/v1/queues/$queue/messages/$msgId?claim_id="
            claim.value.asInstanceOf[JsString].value should startWith (url)
          }
        }
      }
    })

  }

}
