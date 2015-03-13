package io.sphere.cloudqueues

import akka.actor.ActorRef
import akka.pattern.AskableActorRef
import akka.util.Timeout
import io.sphere.cloudqueues.QueueInterface.{ClaimResponse, MessagesAdded, QueueCreationResponse}
import io.sphere.cloudqueues.QueueManager._

import scala.concurrent.Future
import scala.concurrent.duration._


object QueueInterface {

  sealed trait QueueCreationResponse
  case object QueueCreated extends QueueCreationResponse
  case object QueueAlreadyExists extends QueueCreationResponse

  case class MessagesAdded(messages: List[MessageInQueue])


  sealed trait ClaimResponse
  case class ClaimCreated(claim: Claim) extends ClaimResponse
  case object NoMessagesToClain extends ClaimResponse

  case class MessageDeleted(id: MessageId)

}

class QueueInterface(queueManager: ActorRef) {

  val askable = new AskableActorRef(queueManager)
  implicit val timeout: Timeout = 50.milliseconds

  def newQueue(name: QueueName): Future[QueueCreationResponse] =
    (askable ? NewQueue(name)).asInstanceOf[Future[QueueCreationResponse]]

  def addMessages(queue: QueueName, messages: List[Message]): Future[Option[MessagesAdded]] =
    ask(queue, PutNewMessage(messages)).asInstanceOf[Future[Option[MessagesAdded]]]

  def claimMessages(queue: QueueName, ttl: Int, limit: Int): Future[Option[ClaimResponse]] =
    ask(queue, ClaimMessages(ttl, limit)).asInstanceOf[Future[Option[ClaimResponse]]]

  def deleteMessages(queue: QueueName, messageId: MessageId, claimId: Option[ClaimId]): Future[Any] =
    ask(queue, DeleteMessage(messageId, claimId))

  private def ask(queue: QueueName, op: QueueOperation): Future[Any] =
    askable ? AQueueOperation(queue, op)

}