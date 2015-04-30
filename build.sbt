import java.util.Date

import com.typesafe.sbt.packager.archetypes.ServerLoader

name := """cloud-queues-simulator"""

version := "1.0"

scalaVersion := "2.11.6"

val akkaHttpVersion = "1.0-RC1"

libraryDependencies ++=
  "com.typesafe.akka" %% "akka-http-scala-experimental" % akkaHttpVersion ::
  "com.typesafe.akka" %% "akka-slf4j" % "2.3.10" ::
  "com.typesafe.akka" %% "akka-http-spray-json-experimental" % akkaHttpVersion ::
  "com.typesafe" % "config" % "1.2.1" ::
  "com.github.kxbmap" %% "configs" % "0.2.3" ::
  "ch.qos.logback" % "logback-classic" % "1.1.3" ::
  Nil


libraryDependencies ++=
  "org.scalatest" %% "scalatest" % "2.2.4" ::
  "org.scalacheck" %% "scalacheck" % "1.12.2" ::
  "com.typesafe.akka" %% "akka-http-testkit-scala-experimental" % akkaHttpVersion ::
  "org.mockito" % "mockito-core" % "1.10.19" ::
  Nil map (_ % Test)

// Java 8
javacOptions in ThisBuild ++= Seq("-source", "1.8", "-target", "1.8")
initialize := {
  val _ = initialize.value
  if (sys.props("java.specification.version") != "1.8")
    sys.error("Java 8 is required for this project.")
}

spray.revolver.RevolverPlugin.Revolver.settings

enablePlugins(JavaServerAppPackaging)

enablePlugins(DebianPlugin)

version in Debian := new java.text.SimpleDateFormat("yyyyMMddHHmmss").format(new Date)

maintainer := "Sphere Team <support@sphere.io>"

packageSummary := "cloud queues simulator"

packageDescription := """It simulates cloud queues that is based on the openstack zaqar.
 References:
 - http://www.rackspace.com/cloud/queues
 - https://github.com/openstack/zaqar"""

serverLoading in Debian := ServerLoader.SystemV
