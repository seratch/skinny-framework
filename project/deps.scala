import sbt._, Keys._
import scala.language.postfixOps

object deps {

  lazy val scalatraVersion = "2.4.0.RC1"
  lazy val json4SVersion = "3.3.0.RC1"
  lazy val scalikeJDBCVersion = "2.2.6"
  lazy val h2Version = "1.4.187"
  lazy val kuromojiVersion = "5.1.0"
  lazy val mockitoVersion = "1.10.19"
  lazy val jettyVersion = "9.2.10.v20150310"
  lazy val logbackVersion = "1.1.3"
  lazy val slf4jApiVersion = "1.7.12"

  // -----------------------------
  // common dependencies

  lazy val fullExclusionRules = Seq(
    ExclusionRule("org.slf4j", "slf4j-api"),
    ExclusionRule("joda-time", "joda-time"),
    ExclusionRule("org.joda",  "joda-convert")
  )

  lazy val json4sDependencies = Seq(
    "org.json4s"    %% "json4s-jackson"     % json4SVersion    % "compile" excludeAll(fullExclusionRules: _*),
    "org.json4s"    %% "json4s-ext"         % json4SVersion    % "compile" excludeAll(fullExclusionRules: _*)
  )

  lazy val scalatraDependencies = Seq(
    "org.scalatra"  %% "scalatra"             % scalatraVersion  % "compile" excludeAll(fullExclusionRules: _*),
    "org.scalatra"  %% "scalatra-scalate"     % scalatraVersion  % "compile" excludeAll(fullExclusionRules: _*),
    "org.scalatra.scalate"  %% "scalate-core" % "1.7.1"          % "compile" excludeAll(fullExclusionRules: _*),
    "org.scalatra"  %% "scalatra-json"        % scalatraVersion  % "compile" excludeAll(fullExclusionRules: _*),
    "org.scalatra"  %% "scalatra-scalatest"   % scalatraVersion  % "test"
  ) ++ json4sDependencies ++ servletApiDependencies ++ slf4jApiDependencies

  lazy val scalikejdbcDependencies = Seq(
    "org.scalikejdbc" %% "scalikejdbc"                      % scalikeJDBCVersion % "compile" excludeAll(fullExclusionRules: _*),
    "org.scalikejdbc" %% "scalikejdbc-syntax-support-macro" % scalikeJDBCVersion % "compile" excludeAll(fullExclusionRules: _*),
    "org.scalikejdbc" %% "scalikejdbc-config"               % scalikeJDBCVersion % "compile" excludeAll(fullExclusionRules: _*),
    "org.scalikejdbc" %% "scalikejdbc-test"                 % scalikeJDBCVersion % "test"
  )

  lazy val servletApiDependencies = Seq(
    "javax.servlet" % "javax.servlet-api" % "3.1.0"  % "provided"
  )
  lazy val slf4jApiDependencies   = Seq(
    "org.slf4j"     % "slf4j-api"         % slf4jApiVersion % "compile"
  )
  lazy val jodaDependencies = Seq(
    "joda-time"     %  "joda-time"        % "2.7"    % "compile",
    "org.joda"      %  "joda-convert"     % "1.7"    % "compile"
  )
  lazy val mailDependencies = slf4jApiDependencies ++ Seq(
    "javax.mail"              %  "mail"            % "1.4.7"          % "compile",
    "org.jvnet.mock-javamail" %  "mock-javamail"   % "1.9"            % "provided"
  )
  lazy val testDependencies = Seq(
    "org.scalatest"           %% "scalatest"       % "2.2.4"        % "test",
    "org.mockito"             %  "mockito-core"    % mockitoVersion % "test",
    "ch.qos.logback"          %  "logback-classic" % logbackVersion % "test",
    "org.jvnet.mock-javamail" %  "mock-javamail"   % "1.9"          % "test",
    "com.h2database"          %  "h2"              % h2Version      % "test",
    "org.skinny-framework"    %  "skinny-logback"  % "1.0.6"        % "test",
    "com.h2database"          %  "h2"              % h2Version      % "test"
  )

}
