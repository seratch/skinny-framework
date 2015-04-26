import skinny.scalate.ScalatePlugin._, ScalateKeys._
import deps._
import functions._

val currentVersion = "1.4.0-SNAPSHOT"

lazy val baseSettings = Seq(
  organization := "org.skinny-framework",
  version := currentVersion,
  resolvers ++= Seq(
    // for scalatra-specs2
    "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases",
    "sonatype releases"  at "https://oss.sonatype.org/content/repositories/releases"
    //,"sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
  ),
  publishTo <<= version { (v: String) => _publishTo(v) },
  publishMavenStyle := true,
  sbtPlugin := false,
  scalaVersion := "2.11.6",
  ivyScala := ivyScala.value map { _.copy(overrideScalaVersion = true) },
  scalacOptions ++= _scalacOptions,
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { x => false },
  transitiveClassifiers in Global := Seq(Artifact.SourceClassifier),
  incOptions := incOptions.value.withNameHashing(true),
  logBuffered in Test := false,
  javaOptions in Test ++= Seq("-Dskinny.env=test"),
  // TODO: Test failure in velocity module when enabling CachedResolution
  // (java.lang.NoSuchMethodError: javax.servlet.ServletContext.getContextPath()Ljava/lang/String;)
  // updateOptions := updateOptions.value.withCachedResolution(true),
  javacOptions ++= Seq("-source", "1.7", "-target", "1.7", "-encoding", "UTF-8", "-Xlint:-options"),
  javacOptions in doc := Seq("-source", "1.7"),
  pomExtra := _pomExtra
) ++ scalariformSettings ++ net.virtualvoid.sbt.graph.Plugin.graphSettings

// -----------------------------
// skinny libraries

lazy val common = (project in file("common")).settings(baseSettings ++ Seq(
  name := "skinny-common",
  libraryDependencies  <++= (scalaVersion) { (sv) =>
    Seq(
      "com.typesafe"      % "config"                    % "1.2.1"         % "compile",
      "org.apache.lucene" % "lucene-core"               % kuromojiVersion % "provided",
      "org.apache.lucene" % "lucene-analyzers-common"   % kuromojiVersion % "provided",
      "org.apache.lucene" % "lucene-analyzers-kuromoji" % kuromojiVersion % "provided"
    ) ++
    jodaDependencies ++ slf4jApiDependencies ++ testDependencies ++ (sv match {
      case v if v.startsWith("2.11.") => Seq("org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.3" % "compile")
      case _ => Nil
    })
  }
) ++ _jettyOrbitHack)

lazy val httpClient = (project in file("http-client")).settings(baseSettings ++ Seq(
  name := "skinny-http-client",
  libraryDependencies ++= Seq(
    "org.specs2"         %% "specs2-core"        % "2.4.17"     % "test",
    "commons-fileupload" %  "commons-fileupload" % "1.3.1"      % "test",
    "commons-io"         %  "commons-io"         % "2.4"        % "test",
    "commons-httpclient" %  "commons-httpclient" % "3.1"        % "test",
    "javax.servlet"      %  "javax.servlet-api"  % "3.1.0"      % "test",
    "org.eclipse.jetty"  %  "jetty-server"       % jettyVersion % "test",
    "org.eclipse.jetty"  %  "jetty-servlet"      % jettyVersion % "test"
  ) ++ slf4jApiDependencies
) ++ _jettyOrbitHack).dependsOn(common)

lazy val framework = (project in file("framework")).settings(baseSettings ++ Seq(
  name := "skinny-framework",
  libraryDependencies <++= (scalaVersion) { (sv) =>
    scalatraDependencies ++ Seq(
      "commons-io"    %  "commons-io" % "2.4",
      sv match {
        case v if v.startsWith("2.11.") => "org.scalatra.scalate"   %% "scalamd" % "1.6.1"
        case _ =>                          "org.fusesource.scalamd" %% "scalamd" % "1.6"
      }
    ) ++ testDependencies
  }
) ++ _jettyOrbitHack).dependsOn(common, json, validator, orm, mailer, httpClient, worker)

lazy val worker = (project in file("worker")).settings(baseSettings ++ Seq(
  name := "skinny-worker",
  libraryDependencies ++= testDependencies
)).dependsOn(common)

lazy val standalone = (project in file("standalone")).settings(baseSettings ++ Seq(
  name := "skinny-standalone",
  libraryDependencies ++= Seq(
    "javax.servlet"     %  "javax.servlet-api" % "3.1.0"       % "compile",
    "org.eclipse.jetty" %  "jetty-webapp"      % jettyVersion  % "compile",
    "org.eclipse.jetty" %  "jetty-servlet"     % jettyVersion  % "compile",
    "org.eclipse.jetty" %  "jetty-server"      % jettyVersion  % "compile"
  )
) ++ _jettyOrbitHack).dependsOn(framework)

lazy val assets = (project in file("assets")).settings(baseSettings ++ Seq(
  name := "skinny-assets",
  libraryDependencies ++= scalatraDependencies ++ Seq(
    "ro.isdc.wro4j" %  "rhino"      % "1.7R5-20130223-1",
    "commons-io"    %  "commons-io" % "2.4"
  ) ++ testDependencies
)).dependsOn(framework)

lazy val task = (project in file("task")).settings(baseSettings ++ Seq(
  name := "skinny-task",
  libraryDependencies ++= Seq("commons-io" %  "commons-io" % "2.4") ++ testDependencies
)).dependsOn(common, orm % "provided->compile")

lazy val orm = (project in file("orm")).settings(baseSettings ++ Seq(
  name := "skinny-orm",
  libraryDependencies ++= scalikejdbcDependencies ++ servletApiDependencies ++ Seq(
    "org.flywaydb"    %  "flyway-core"    % "3.2.1"       % "compile",
    "org.hibernate"   %  "hibernate-core" % "4.3.9.Final" % "test"
  ) ++ testDependencies
)).dependsOn(common)

lazy val factoryGirl = (project in file("factory-girl")).settings(baseSettings ++ Seq(
  name := "skinny-factory-girl",
  libraryDependencies <++= (scalaVersion) { (sv) =>
    scalikejdbcDependencies ++ Seq(
      "org.scala-lang" % "scala-compiler" % sv
    ) ++ testDependencies
  }
)).dependsOn(common, orm)

lazy val freemarker = (project in file("freemarker")).settings(baseSettings ++ Seq(
  name := "skinny-freemarker",
  libraryDependencies ++= scalatraDependencies ++ Seq(
    "commons-beanutils" %  "commons-beanutils"  % "1.9.2"   % "compile",
    "org.freemarker"    %  "freemarker"         % "2.3.22"  % "compile"
  ) ++ testDependencies
) ++ _jettyOrbitHack).dependsOn(framework)

lazy val thymeleaf = (project in file("thymeleaf")).settings(baseSettings ++ Seq(
  name := "skinny-thymeleaf",
  libraryDependencies ++= scalatraDependencies ++ Seq(
    "org.thymeleaf"            %  "thymeleaf"                % "2.1.4.RELEASE" % "compile",
    "nz.net.ultraq.thymeleaf"  %  "thymeleaf-layout-dialect" % "1.2.8"         % "compile" exclude("org.thymeleaf", "thymeleaf"),
    "net.sourceforge.nekohtml" %  "nekohtml"                 % "1.9.22"        % "compile"
  ) ++ testDependencies
) ++ _jettyOrbitHack).dependsOn(framework)

lazy val velocity = (project in file("velocity")).settings(baseSettings ++ Seq(
  name := "skinny-velocity",
  libraryDependencies ++= scalatraDependencies ++ Seq(
    "commons-logging"     % "commons-logging" % "1.2"   % "compile",
    "org.apache.velocity" % "velocity"        % "1.7"   % "compile",
    "org.apache.velocity" % "velocity-tools"  % "2.0"   % "compile" excludeAll(
      ExclusionRule("org.apache.velocity", "velocity"),
      ExclusionRule("commons-loggin", "commons-logging")
    )
  ) ++ testDependencies
) ++ _jettyOrbitHack).dependsOn(framework)

lazy val scaldi = (project in file("scaldi")).settings(baseSettings ++ Seq(
  name := "skinny-scaldi",
  libraryDependencies <++= (scalaVersion) { (sv) => scalatraDependencies ++
    Seq(
      sv match {
        case v if v.startsWith("2.10.") => "org.scaldi" %% "scaldi" % "0.3.2"
        case _ =>                          "org.scaldi" %% "scaldi" % "0.5.4"
      }
    ) ++ testDependencies
  }
)).dependsOn(framework)

lazy val json = (project in file("json")).settings(baseSettings ++ Seq(
  name := "skinny-json",
  libraryDependencies ++= json4sDependencies ++ jodaDependencies ++ testDependencies
))

lazy val oauth2 = (project in file("oauth2")).settings(baseSettings ++ Seq(
  name := "skinny-oauth2",
  libraryDependencies ++= Seq(
    "org.apache.oltu.oauth2" %  "org.apache.oltu.oauth2.client" % "1.0.0" % "compile" exclude("org.slf4j", "slf4j-api")
  ) ++ servletApiDependencies ++ testDependencies
)).dependsOn(common, json)

lazy val oauth2Controller = (project in file("oauth2-controller")).settings(baseSettings ++ Seq(
  name := "skinny-oauth2-controller",
  libraryDependencies ++= servletApiDependencies
)).dependsOn(framework, oauth2)

lazy val twitterController = (project in file("twitter-controller")).settings(baseSettings ++ Seq(
  name := "skinny-twitter-controller",
  libraryDependencies ++= Seq(
    "org.twitter4j" % "twitter4j-core" % "4.0.3" % "compile"
  ) ++ servletApiDependencies
)).dependsOn(framework)

lazy val logback = (project in file("logback")).settings(baseSettings ++ Seq(
  name             := "skinny-logback",
  version          := "1.0.7-SNAPSHOT",
  crossPaths       := false,
  autoScalaLibrary := false,
  libraryDependencies ++= Seq(
    "ch.qos.logback" % "logback-classic" % logbackVersion  % "compile" exclude("org.slf4j", "slf4j-api"),
    "org.slf4j"      % "slf4j-api"       % slf4jApiVersion % "compile"
  )
))

lazy val validator = (project in file("validator")).settings(baseSettings ++ Seq(
  name := "skinny-validator",
  libraryDependencies ++= jodaDependencies ++ testDependencies
)).dependsOn(common)

lazy val mailer = (project in file("mailer")).settings(baseSettings ++ Seq(
  name := "skinny-mailer",
  libraryDependencies ++= mailDependencies ++ testDependencies
)).dependsOn(common)

lazy val test = (project in file("test")).settings(baseSettings ++ Seq(
  name := "skinny-test",
  libraryDependencies ++= scalatraDependencies ++ mailDependencies ++ testDependencies ++ Seq(
    "org.mockito"     %  "mockito-core"       % mockitoVersion     % "compile"  exclude("org.slf4j", "slf4j-api"),
    "org.scalikejdbc" %% "scalikejdbc-test"   % scalikeJDBCVersion % "compile"  exclude("org.slf4j", "slf4j-api"),
    "org.scalatra"    %% "scalatra-specs2"    % scalatraVersion    % "provided",
    "org.scalatra"    %% "scalatra-scalatest" % scalatraVersion    % "provided"
  )
) ++ _jettyOrbitHack).dependsOn(framework)

// -----------------------------
// example and tests with a real project

lazy val example = (project in file("example")).settings(baseSettings ++ scalateSettings ++ Seq(
  name := "skinny-framework-example",
  libraryDependencies ++= Seq(
    "com.h2database"     %  "h2"                 % h2Version,
    "ch.qos.logback"     %  "logback-classic"    % logbackVersion,
    "org.scalatra"       %% "scalatra-specs2"    % scalatraVersion       % "test",
    "org.scalatra"       %% "scalatra-scalatest" % scalatraVersion       % "test",
    "org.mockito"        %  "mockito-core"       % mockitoVersion        % "test",
    "org.eclipse.jetty"  %  "jetty-servlet"      % jettyVersion          % "container;provided;test",
    "org.eclipse.jetty"  %  "jetty-webapp"       % jettyVersion          % "container",
    "org.eclipse.jetty"  %  "jetty-plus"         % jettyVersion          % "container",
    "javax.servlet"      %  "javax.servlet-api"  % "3.1.0"               % "container;provided;test"
  ),
  mainClass := Some("TaskLauncher"),
  // Scalatra tests become slower when multiple controller tests are loaded in the same time
  parallelExecution in Test := false,
  unmanagedClasspath in Test <+= (baseDirectory) map { bd =>  Attributed.blank(bd / "src/main/webapp") }
)).dependsOn(
  framework,
  assets,
  logback,
  thymeleaf,
  freemarker,
  velocity,
  factoryGirl,
  test,
  task,
  scaldi,
  oauth2Controller,
  twitterController)

