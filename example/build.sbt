import scala.language.postfixOps

scalariformSettings

net.virtualvoid.sbt.graph.Plugin.graphSettings

jetty(port = 8080)

initialCommands := """
import _root_.controller._, model._
import org.joda.time._
import scalikejdbc._, SQLInterpolation._, config._
DBsWithEnv("development").setupAll()
implicit val session = AutoSession
"""

