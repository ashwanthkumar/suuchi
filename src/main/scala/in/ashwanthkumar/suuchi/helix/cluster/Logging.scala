package in.ashwanthkumar.suuchi.helix.cluster

import org.slf4j.LoggerFactory

trait Logging {
  self =>
  val log = LoggerFactory.getLogger(self.getClass)
}
