package in.ashwanthkumar.suuchi.utils

import org.slf4j.LoggerFactory

trait Logging { self =>
  val log = LoggerFactory.getLogger(this.getClass)
}
