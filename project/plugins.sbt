addDependencyTreePlugin

addSbtPlugin("se.marcuslonnberg"             % "sbt-docker"                % "1.8.3")
addSbtPlugin("com.eed3si9n"                  % "sbt-assembly"              % "1.1.0")
addSbtPlugin("com.williamhill.bettingengine" % "be-sbt-utils"              % "2.2.2")
addSbtPlugin("org.scoverage"                 % "sbt-scoverage"             % "1.9.3")
addSbtPlugin("org.scalameta"                 % "sbt-scalafmt"              % "2.4.6")
addSbtPlugin("ch.epfl.scala"                 % "sbt-scalafix"              % "0.9.34")
addSbtPlugin("io.kamon"                      % "sbt-kanela-runner"         % "2.0.12") // Only required when running from SBT
addSbtPlugin("io.github.davidgregory084"     % "sbt-tpolecat"              % "0.1.20")
addSbtPlugin("com.github.cb372"              % "sbt-explicit-dependencies" % "0.2.16")

val nexusUrl = sys.env.getOrElse("WH_NEXUS_REPO", "https://nexus.dtc.prod.williamhill.plc:8443/repository")

def resolver(name: String): Resolver =
  Resolver.url(s"Artifactory Realm ${name.capitalize}", url(s"$nexusUrl/$name"))(Resolver.ivyStylePatterns)

resolvers += resolver("ivy-releases")
resolvers += resolver("ivy-snapshots")
