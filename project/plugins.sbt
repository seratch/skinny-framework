resolvers += Classpaths.sbtPluginReleases
addMavenResolverPlugin

addSbtPlugin("org.scalatra.sbt"     % "scalatra-sbt"         % "0.4.0")
addSbtPlugin("com.earldouglas"      % "xsbt-web-plugin"      % "1.1.0")
addSbtPlugin("org.skinny-framework" % "sbt-scalate-precompiler" % "1.7.1.0")

addSbtPlugin("com.typesafe.sbt"     % "sbt-scalariform"      % "1.3.0")
addSbtPlugin("com.github.mpeltonen" % "sbt-idea"             % "1.6.0")
addSbtPlugin("com.jsuereth"         % "sbt-pgp"              % "1.0.0")
addSbtPlugin("net.virtual-void"     % "sbt-dependency-graph" % "0.7.5")
addSbtPlugin("com.timushev.sbt"     % "sbt-updates"          % "0.1.8")
addSbtPlugin("org.scoverage"        % "sbt-scoverage"        % "1.0.4")
addSbtPlugin("org.scoverage"        % "sbt-coveralls"        % "1.0.0.BETA1")

scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")
