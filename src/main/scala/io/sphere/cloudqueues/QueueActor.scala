package io.sphere.cloudqueues

import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import io.sphere.cloudqueues.QueueInterface._
import io.sphere.cloudqueues.QueueManager._



object QueueManager {


  case class NewQueue(queue: QueueName)


  sealed trait QueueOperation
  case class PutNewMessage(messages: List[Message]) extends QueueOperation
  case class ClaimMessages(ttl: Int, limit: Int) extends QueueOperation
  case class DeleteMessage(id: MessageId, claimId: Option[ClaimId]) extends QueueOperation

  case class AQueueOperation[T <: QueueOperation](queue: QueueName, operation: T)

}

class QueueManager extends Actor with ActorLogging {

  private var queues = Map.empty[QueueName, ActorRef]

  def receive = {
    case NewQueue(name) ⇒
      if (queues.contains(name)) {
        log.info(s"queue '$name' already exists")
        sender ! QueueAlreadyExists
      } else {
        log.info(s"creates queue '$name'")
        queues += name → context.actorOf(QueueActor.props(name), name.name)
        sender ! QueueCreated
      }

    case AQueueOperation(queue, operation) ⇒
      if (!queues.contains(queue)) {
        log.debug(s"the queue '$queue' does not exist")
        sender ! None
      } else queues(queue).forward(operation)

  }
}

object QueueActor {
  def props(name: QueueName) = Props(new QueueActor(name))
}

class QueueActor(name: QueueName) extends Actor with ActorLogging {

  import collection.mutable.{ArrayBuffer, Stack}

  private var queuedMessages = new Stack[MessageInQueue]()
  private var claims = new ArrayBuffer[Claim]()

  def receive: Receive = {
    case op: QueueOperation ⇒ handle(op)
  }

  private def handle(op: QueueOperation) = op match {

    case PutNewMessage(messages) ⇒
      log.info(s"putting ${messages.size} messages in '$name'")
      val addedMessages = messages.map(MessageInQueue.apply)
      queuedMessages.pushAll(addedMessages)
      sender ! Some(MessagesAdded(addedMessages))


    case ClaimMessages(ttl, limit) ⇒
      val nbr = Math.min(limit, queuedMessages.length)
      val messages = (1 to nbr).map(_ ⇒ queuedMessages.pop())
      if (messages.nonEmpty) {
        val claim = Claim(ClaimId(UUID.randomUUID().toString), messages.toList)
        claims.append(claim)
        log.info(s"claimed ${messages.size} from '$name' with claim id '${claim.id}'")
        sender ! Some(ClaimCreated(claim))
      } else {
        log.info(s"no messages to claim in '$name'")
        sender ! Some(NoMessagesToClain)
      }



    case DeleteMessage(msgId, None) ⇒
      queuedMessages = queuedMessages.filterNot(_.id == msgId)
      sender ! Some(MessageDeleted)


    case DeleteMessage(msgId, Some(claimId)) ⇒
      val newClaim = for {
        claim ← claims.find(_.id == claimId)
        msg ← claim.messages.find(_.id == msgId)
      } yield {
          claim.copy(messages = claim.messages.filterNot(_.id == msgId))
        }
      sender ! (newClaim map { c ⇒
        log.info(s"remove message '$msgId' from claim '$claimId'")
        claims = claims.filterNot(_.id == claimId)
        claims.append(c)
        MessageDeleted
      })
  }
}