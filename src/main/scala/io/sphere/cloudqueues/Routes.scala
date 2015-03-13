package io.sphere.cloudqueues

import akka.http.marshallers.sprayjson.SprayJsonSupport._
import akka.http.model.ContentTypes._
import akka.http.model.StatusCodes._
import akka.http.model.headers.Location
import akka.http.model.{HttpEntity, HttpResponse}
import akka.http.server.Directives._
import akka.http.server.Route
import akka.stream.ActorFlowMaterializer
import io.sphere.cloudqueues.QueueInterface._
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.concurrent.ExecutionContext


object Routes {

  def index(implicit ec: ExecutionContext): Route =
    get {
      path("") {
        complete("hello")
      }
    }

  def auth(implicit ec: ExecutionContext): Route =
    path("v2.0" / "tokens") {
      post {
        val ast =
          JsObject("access" →
            JsObject("token" →
              JsObject(
                "id" → JsString("simulated-token"),
                "expires" → JsString("2212-04-13T22:51:02.000-06:00"))))
        complete(ast)
      }
    }

  case class ClaimRequestBody(ttl: Int, grace: Int)
  object ClaimRequestBody {
    implicit val format = jsonFormat2(ClaimRequestBody.apply)
  }

  case class Queue(queueInterface: QueueInterface)(implicit ec: ExecutionContext, materializer: ActorFlowMaterializer) {

    implicit val messageJson = jsonFormat2(Message.apply)

    val newQueue = put {
      path(Segment) { name ⇒
        onSuccess(queueInterface.newQueue(QueueName(name))) { resp ⇒
          val status = resp match {
            case QueueAlreadyExists ⇒ NoContent
            case QueueCreated ⇒ Created
          }
          complete(HttpResponse(status = status).withHeaders(Location(s"/v1/queues/$name")))
        }
      }
    }

    val postMessages = post {
      path(Segment / "messages") { name ⇒
        entity(as[List[Message]]) { messages =>
          onSuccess(queueInterface.addMessages(QueueName(name), messages)) {
            case None ⇒ complete(HttpResponse(status = NotFound))
            case Some(MessagesAdded(msg)) ⇒
              val response = JsObject(
                "partial" → JsBoolean(false),
                "resources" → JsArray(msg.map(m ⇒ JsString(s"/v1/queues/$name/messages/${m.id}")): _*)
              )
              complete(Created → response)
          }
        }
      }
    }

    val claimMessages = post {
      path(Segment / "claims") { name ⇒
        parameter('limit.as[Int] ?) { maybeLimit ⇒
          val limit = maybeLimit getOrElse 10
          entity(as[ClaimRequestBody]) { claim ⇒
            onSuccess(queueInterface.claimMessages(QueueName(name), claim.ttl, limit)) {
              case None ⇒ complete(HttpResponse(status = NotFound))
              case Some(NoMessagesToClain) ⇒ complete(HttpResponse(status = NoContent))
              case Some(ClaimCreated(Claim(id, msgs))) ⇒
                val ast = JsArray(msgs.map(m ⇒ JsObject(
                  "body" → m.json,
                  "age" → JsNumber(239),
                  "href" → JsString(s"/v1/queues/$name/messages/${m.id}?claim_id=$id"),
                  "ttl" → JsNumber(claim.ttl))): _*)
                complete(Created → ast)
            }
          }
        }
      }
    }

    val deleteMessages = delete {
      path(Segment / "messages" / Segment) { (name, msgId) ⇒
        parameter('claim_id.as[String] ?) { claimId ⇒
          // TODO (YaSi): parse claimId directly with type ClaimId
          onSuccess(queueInterface.deleteMessages(QueueName(name), MessageId(msgId), claimId.map(ClaimId.apply))) { _ ⇒
            complete(HttpResponse(status = NoContent, entity = HttpEntity.empty(`application/json`)))
          }
        }
      }
    }

    val route: Route = pathPrefix("v1" / "queues") {
      newQueue ~ postMessages ~ claimMessages ~ deleteMessages
    }
  }

}
