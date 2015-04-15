package io.sphere.cloudqueues

import akka.http.marshallers.sprayjson.SprayJsonSupport._
import akka.http.model.ContentTypes._
import akka.http.model.StatusCodes._
import akka.http.model.headers.Location
import akka.http.model.{HttpEntity, HttpResponse}
import akka.http.server.Directives._
import akka.http.server.{RequestContext, Route}
import akka.stream.ActorFlowMaterializer
import io.sphere.cloudqueues.QueueInterface._
import io.sphere.cloudqueues.oauth.OAuth
import io.sphere.cloudqueues.oauth.OAuth._
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.concurrent.ExecutionContext


object Routes {

  def index(implicit ec: ExecutionContext): Route =
    path("") {
      get {
        complete("cloud queues simulator")
      }
    }

  case class Auth(oauth: OAuth)(implicit ec: ExecutionContext) {

    val route =
      path("v2.0" / "tokens") {
        post {
          val validUntil = oauth.defaultValidityDate
          val validUntilString = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(validUntil)
          val token = oauth.createOAuthToken(validUntil = validUntil)
          val ast =
            JsObject("access" →
              JsObject("token" →
                JsObject(
                  "id" → JsString(token.token),
                  "expires" → JsString(validUntilString))))
          complete(ast)
        }
      }

  }

  case class ClaimRequestBody(ttl: Int, grace: Int)
  object ClaimRequestBody {
    implicit val format = jsonFormat2(ClaimRequestBody.apply)
  }

  case class Queue(queueInterface: QueueInterface, oauth: OAuth)(implicit ec: ExecutionContext, materializer: ActorFlowMaterializer) {

    implicit val messageJson = jsonFormat2(Message.apply)

    val newQueue =
      path(Segment) { name ⇒
        put {
          onSuccess(queueInterface.newQueue(QueueName(name))) { resp ⇒
            val status = resp match {
              case QueueAlreadyExists ⇒ NoContent
              case QueueCreated ⇒ Created
            }
            complete(HttpResponse(status = status).withHeaders(Location(s"/v1/queues/$name")))
          }
        }
      }

    val postMessages =
      path(Segment / "messages") { name ⇒
        post {
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

    val claimMessages =
      path(Segment / "claims") { name ⇒
        post {
          parameter('limit.as[Int] ?) { maybeLimit ⇒
            val limit = maybeLimit getOrElse 10
            entity(as[ClaimRequestBody]) { claim ⇒
              onSuccess(queueInterface.claimMessages(QueueName(name), claim.ttl, limit)) {
                case None ⇒ complete(HttpResponse(status = NotFound))
                case Some(NoMessagesToClaim) ⇒ complete(HttpResponse(status = NoContent))
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

    val releaseClaim =
      path(Segment / "claims" / Segment) { (queueName, claimId) ⇒
        delete {
          onSuccess(queueInterface.releaseClaim(QueueName(queueName), ClaimId(claimId))) {
            case None ⇒ complete(HttpResponse(status = NotFound))
            case Some(ClaimReleased) ⇒ complete(HttpResponse(status = NoContent))
          }
        }
      }

    val deleteMessages =
      path(Segment / "messages" / Segment) { (name, msgId) ⇒
        delete {
          parameter('claim_id.as[String] ?) { claimId ⇒
            // TODO (YaSi): parse claimId directly with type ClaimId
            onSuccess(queueInterface.deleteMessages(QueueName(name), MessageId(msgId), claimId.map(ClaimId.apply))) { _ ⇒
              complete(HttpResponse(status = NoContent, entity = HttpEntity.empty(`application/json`)))
            }
          }
        }
      }

    val authenticated: RequestContext ⇒ Boolean = req ⇒ {
      req.request.getHeader("X-Auth-Token").fold(false) { token ⇒
        oauth.validates(OAuthToken(token.value())) match {
          case OAuthValid(_) ⇒ true
          case _ ⇒ false
        }
      }
    }

    val route: Route = pathPrefix("v1" / "queues") {
      authorize(authenticated) {
        newQueue ~ postMessages ~ claimMessages ~ releaseClaim ~ deleteMessages
      }
    }
  }

}
