package io.sphere.cloudqueues

import akka.http.scaladsl.model.headers.{ModeledCustomHeader, ModeledCustomHeaderCompanion}

import scala.util.{Success, Try}

object `X-Auth-Token` extends ModeledCustomHeaderCompanion[`X-Auth-Token`] {
  override val name: String = "X-Auth-Token"
  override def parse(value: String): Try[`X-Auth-Token`] = Success(new `X-Auth-Token`(value))
}

final case class `X-Auth-Token`(value: String) extends ModeledCustomHeader[`X-Auth-Token`] {
  override val companion = `X-Auth-Token`

  override val renderInResponses = true
  override val renderInRequests = true
}
