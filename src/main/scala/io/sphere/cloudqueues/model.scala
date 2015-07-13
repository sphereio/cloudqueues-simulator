package io.sphere.cloudqueues

import java.util.UUID

import akka.http.scaladsl.unmarshalling.Unmarshaller
import akka.http.scaladsl.server.PathMatchers.Segment
import spray.json.JsValue


//
// model
//
case class QueueName(name: String) extends AnyVal {
  override def toString: String = name
}
object QueueName {
  val segment = Segment map apply
}

case class MessageId(id: String) extends AnyVal {
  override def toString: String = id
}
object MessageId {
  val segment = Segment map apply
}

case class ClaimId(id: String) extends AnyVal {
  override def toString: String = id
}
object ClaimId {
  val segment = Segment map apply
  // ClaimId can be directly read as query parameter
  implicit val claimUnmarshaller = Unmarshaller.strict[String, ClaimId](ClaimId.apply)
}

case class Message(ttl: Int, body: JsValue)
case class MessageInQueue(id: MessageId, ttl: Int, json: JsValue)
object MessageInQueue {
  def apply(message: Message): MessageInQueue = MessageInQueue(MessageId(UUID.randomUUID().toString), message.ttl, message.body)
}

case class Claim(id: ClaimId, messages: Vector[MessageInQueue])

