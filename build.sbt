import java.util.Date

import com.typesafe.sbt.packager.archetypes.ServerLoader

name := """cloud-queues-simulator"""

version := "1.0"

scalaVersion := "2.11.6"

val akkaHttpVersion = "1.0-M5"

libraryDependencies ++=
  "com.typesafe.akka" %% "akka-http-experimental" % akkaHttpVersion ::
  "com.typesafe.akka" %% "akka-slf4j" % "2.3.9" ::
  "com.typesafe.akka" %% "akka-http-spray-json-experimental" % akkaHttpVersion ::
  "com.typesafe" % "config" % "1.2.1" ::
  "com.github.kxbmap" %% "configs" % "0.2.3" ::
  "ch.qos.logback" % "logback-classic" % "1.1.3" ::
  Nil


libraryDependencies ++=
  "org.scalatest" %% "scalatest" % "2.2.4" ::
  "org.scalacheck" %% "scalacheck" % "1.12.2" ::
  "com.typesafe.akka" %% "akka-http-testkit-experimental" % akkaHttpVersion ::
  "org.mockito" % "mockito-core" % "1.10.19" ::
  Nil map (_ % Test)

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
