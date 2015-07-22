package io.sphere.cloudqueues

import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import io.sphere.cloudqueues.QueueInterface._
import io.sphere.cloudqueues.QueueManager._
import io.sphere.cloudqueues.reply.Replyable


object QueueManager {


  case class NewQueue(queue: QueueName) extends Replyable[QueueCreationResponse]


  sealed trait QueueOperation[R] extends Replyable[R]
  case class PutNewMessage(messages: List[Message]) extends QueueOperation[Option[MessagesAdded]]
  case class ClaimMessages(ttl: Int, limit: Int) extends QueueOperation[Option[ClaimResponse]]
  case class DeleteMessage(id: MessageId, claimId: Option[ClaimId]) extends QueueOperation[Option[MessageDeleted]]
  case class ReleaseClaim(claimId: ClaimId) extends QueueOperation[Option[ClaimReleased.type]]

  case class AQueueOperation[R](queue: QueueName, operation: QueueOperation[R]) extends Replyable[R]

}

class QueueManager extends Actor with ActorLogging {

  private val queues = collection.mutable.AnyRefMap[String, ActorRef]()

  def receive = {
    case NewQueue(q) ⇒
      if (queues.contains(q.name)) {
        log.info(s"[$q] queue already exists")
        sender ! QueueAlreadyExists
      } else {
        log.info(s"[$q] create queue")
        queues += q.name → context.actorOf(QueueActor.props(q), q.name)
        sender ! QueueCreated
      }

    case AQueueOperation(queue, operation) ⇒
      queues.get(queue.name) match {
        case None =>
          log.warning(s"[$queue] the queue does not exist. Cannot proceed operation: $operation")
          sender ! None
        case Some(q) => q.forward(operation)
      }

  }
}

object QueueActor {
  def props(name: QueueName) = Props(new QueueActor(name))
}

class QueueActor(name: QueueName) extends Actor with ActorLogging {
  import context._

  import collection.mutable.{ArrayBuffer, Stack}

  private var queuedMessages = new Stack[MessageInQueue]()
  private var claims = new ArrayBuffer[Claim]()

  self ! "check"
  import scala.concurrent.duration._

  def receive: Receive = {
    case "check" ⇒
      val queueSize = queuedMessages.size
      val claimSize = claims.map(_.messages.size).sum
      val message = s"[$name] $queueSize message(s) in queue, $claimSize claimed message(s)"
      if (queueSize == 0 && claimSize == 0) log.debug(message) else log.info(message)
      system.scheduler.scheduleOnce(10.seconds) {
        self ! "check"
      }
    case op: QueueOperation[_] ⇒ handle(op)
  }

  private def handle(op: QueueOperation[_]) = op match {

    case PutNewMessage(messages) ⇒
      log.info(s"[$name] putting ${messages.size} messages")
      val addedMessages = messages.map(MessageInQueue.apply)
      queuedMessages.pushAll(addedMessages)
      sender ! Some(MessagesAdded(addedMessages))


    case ClaimMessages(ttl, limit) ⇒
      val nbr = Math.min(limit, queuedMessages.length)
      val messages = (1 to nbr).map(_ ⇒ queuedMessages.pop())
      if (messages.nonEmpty) {
        val claim = Claim(ClaimId(UUID.randomUUID().toString), messages.toVector)
        claims.append(claim)
        log.info(s"[$name] [${claim.id}] claimed ${messages.size} message")
        sender ! Some(ClaimCreated(claim))
      } else {
        log.debug(s"[$name] no messages to claim")
        sender ! Some(NoMessagesToClaim)
      }


    case ReleaseClaim(claimId) ⇒
      val maybeClaim = claims.find(_.id == claimId)
      val result = maybeClaim map { claim ⇒
        log.info(s"[$name] [${claim.id}] release ${claim.messages.size} messages")
        queuedMessages.pushAll(claim.messages)
        claims = claims.filterNot(_.id == claimId)
        ClaimReleased
      }
      sender ! result


    case DeleteMessage(msgId, None) ⇒
      if (!queuedMessages.exists(_.id == msgId))
        log.error(s"[$name] cannot delete claimed message '$msgId'")
      queuedMessages = queuedMessages.filterNot(_.id == msgId)
      sender ! Some(MessageDeleted)


    case DeleteMessage(msgId, Some(claimId)) ⇒
      val newClaim = for {
        claim ← claims.find(_.id == claimId)
        msg ← claim.messages.find(_.id == msgId)
        newMessagesInClaim = claim.messages.filterNot(_.id == msgId)
        newClaim <- if (newMessagesInClaim.size == claim.messages.size) {
          log.error(s"[$name] [$claimId] cannot delete claimed message '$msgId'")
          None
        } else Some(claim.copy(messages = newMessagesInClaim))
      } yield newClaim

      sender ! (newClaim map { c ⇒
        log.info(s"[$name] [$claimId] remove message '$msgId'. New claim size: ${c.messages.size}")
        claims = claims.filterNot(_.id == claimId)
        if (c.messages.nonEmpty) claims.append(c)
        MessageDeleted
      })
  }
}