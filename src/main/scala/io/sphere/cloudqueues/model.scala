package io.sphere.cloudqueues

import java.util.UUID

import spray.json.JsValue


//
// model
//

case class QueueName(name: String) extends AnyVal {
  override def toString: String = name
}
case class MessageId(id: String) extends AnyVal {
  override def toString: String = id
}
case class ClaimId(id: String) extends AnyVal {
  override def toString: String = id
}

case class Message(ttl: Int, body: JsValue)
case class MessageInQueue(id: MessageId, ttl: Int, json: JsValue)
object MessageInQueue {
  def apply(message: Message): MessageInQueue = MessageInQueue(MessageId(UUID.randomUUID().toString), message.ttl, message.body)
}

case class Claim(id: ClaimId, messages: List[MessageInQueue])

