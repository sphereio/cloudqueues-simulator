import java.util.Date

import com.typesafe.sbt.packager.archetypes.ServerLoader
import com.typesafe.sbt.packager.docker._

name := """cloud-queues-simulator"""

version := "1.0"

scalaVersion := "2.11.7"

enablePlugins(GatlingPlugin)


val akkaHttpVersion = "2.4.2"

libraryDependencies ++=
  "com.typesafe.akka" %% "akka-http-experimental"            % akkaHttpVersion ::
  "com.typesafe.akka" %% "akka-slf4j"                        % "2.4.2"         ::
  "com.typesafe.akka" %% "akka-http-spray-json-experimental" % akkaHttpVersion ::
  "com.typesafe"      %  "config"                            % "1.3.0"         ::
  "com.github.kxbmap" %% "configs"                           % "0.3.0"         ::
  "ch.qos.logback"    %  "logback-classic"                   % "1.1.5"         ::
  Nil


libraryDependencies ++=
  "org.scalatest"     %% "scalatest"         % "2.2.6"         ::
  "org.scalacheck"    %% "scalacheck"        % "1.12.5"        ::
  "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion ::
  "org.mockito"       %  "mockito-core"      % "1.10.19"       ::
  Nil map (_ % Test)



libraryDependencies ++= {
  val gatlingVersion = "2.1.7"

  "io.gatling.highcharts" % "gatling-charts-highcharts" % gatlingVersion ::
  "io.gatling"            % "gatling-test-framework"    % gatlingVersion ::
  Nil map (_ % "it")
}

// compiler warnings
scalacOptions ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-unchecked",
  "-feature",
  "-language:postfixOps",
  "-Xlint",
  "-Ywarn-unused-import")
javacOptions ++= Seq("-deprecation", "-Xlint:unchecked")


// Java 8
javacOptions in ThisBuild ++= Seq("-source", "1.8", "-target", "1.8")
initialize := {
  val _ = initialize.value
  if (sys.props("java.specification.version") != "1.8")
    sys.error("Java 8 is required for this project.")
}

enablePlugins(JavaServerAppPackaging)

enablePlugins(DockerPlugin)
// use a more minimal image https://hub.docker.com/r/develar/java/
dockerBaseImage := "develar/java"
dockerCommands := Seq(
  dockerCommands.value.head,
  // Install bash to be able to start the application
  Cmd("RUN apk add --update bash && rm -rf /var/cache/apk/*")
) ++ dockerCommands.value.tail

dockerExposedPorts := 30001 :: Nil
packageName in Docker := "sphereio/cloud-queues-simulator"
dockerUpdateLatest := true

enablePlugins(DebianPlugin)
version in Debian := new java.text.SimpleDateFormat("yyyyMMddHHmmss").format(new Date)
maintainer := "Sphere Team <support@sphere.io>"
packageSummary := "cloud queues simulator"
packageDescription := """It simulates cloud queues that is based on the openstack zaqar.
 References:
 - http://www.rackspace.com/cloud/queues
 - https://github.com/openstack/zaqar"""
serverLoading in Debian := ServerLoader.SystemV
debianPackageDependencies in Debian += "java8-runtime"
