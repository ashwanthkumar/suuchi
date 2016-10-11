package in.ashwanthkumar.suuchi.utils

import org.slf4j.LoggerFactory

import scala.util.Try

trait Logging { self =>
  val log = LoggerFactory.getLogger(this.getClass)


  def logOnError[T](f: () => T): Try[T] = {
    Try {
      f()
    } recover {
      case e: Exception =>
        log.error(e.getMessage, e)
        throw e
    }
  }
}
