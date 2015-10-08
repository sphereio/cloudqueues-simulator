import java.util.Date

import com.typesafe.sbt.packager.archetypes.ServerLoader

name := """cloud-queues-simulator"""

version := "1.0"

scalaVersion := "2.11.7"

enablePlugins(GatlingPlugin)


val akkaHttpVersion = "1.0"

libraryDependencies ++=
  "com.typesafe.akka" %% "akka-http-experimental"            % akkaHttpVersion ::
  "com.typesafe.akka" %% "akka-slf4j"                        % "2.4.0"         ::
  "com.typesafe.akka" %% "akka-http-spray-json-experimental" % akkaHttpVersion ::
  "com.typesafe"      %  "config"                            % "1.3.0"         ::
  "com.github.kxbmap" %% "configs"                           % "0.3.0"         ::
  "ch.qos.logback"    %  "logback-classic"                   % "1.1.3"         ::
  Nil


libraryDependencies ++=
  "org.scalatest"     %% "scalatest"                      % "2.2.5"         ::
  "org.scalacheck"    %% "scalacheck"                     % "1.12.5"        ::
  "com.typesafe.akka" %% "akka-http-testkit-experimental" % akkaHttpVersion ::
  "org.mockito"       %  "mockito-core"                   % "1.10.19"       ::
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

spray.revolver.RevolverPlugin.Revolver.settings

enablePlugins(JavaServerAppPackaging)

enablePlugins(DockerPlugin)
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
