package io.sphere.cloudqueues

import org.slf4j.{LoggerFactory, Logger}

trait Logging {

  val log: Logger = LoggerFactory getLogger getClass.getName

}
