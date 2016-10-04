import java.util.Collections

import kr.motd.maven.os.Detector

object OSDetector {
  private val impl = new Detector {
    val detectedProperties = System.getProperties

    detect(detectedProperties, Collections.emptyList())
    override def log(s: String): Unit = {}
    override def logProperty(s: String, s1: String): Unit = {}
  }

  def getClassifier = impl.detectedProperties.getProperty(Detector.DETECTED_CLASSIFIER)

  def getOs = impl.detectedProperties.getProperty(Detector.DETECTED_NAME)

  def getArch = impl.detectedProperties.get(Detector.DETECTED_ARCH)
}
