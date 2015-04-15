package io.sphere.cloudqueues.util

import akka.util.Timeout

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

trait FutureAwaits {

  /**
   * Block until a Promise is redeemed.
   */
  def await[T](future: Future[T])(implicit timeout: Timeout = 20.seconds): T =
    Await.result(future, timeout.duration)

}

object FutureAwaits extends FutureAwaits
