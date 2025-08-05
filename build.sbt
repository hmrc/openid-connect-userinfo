import play.sbt.PlayImport.PlayKeys.playDefaultPort
import sbt.Keys.scalacOptions
import uk.gov.hmrc.DefaultBuildSettings

import scala.collection.Seq

val appName = "openid-connect-userinfo"
ThisBuild / majorVersion := 1
ThisBuild / scalaVersion := "3.3.4"

lazy val microservice = Project(appName, file("."))
  .enablePlugins(PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin) // Required to prevent https://github.com/scalatest/scalatest/issues/1427
  .settings(
    playDefaultPort := 9836,
    libraryDependencies ++= AppDependencies(),
    scalacOptions ++= Seq(
      "-Werror",
      "-feature",
      "-Wconf:src=views/.*&msg=unused import:silent",
      "-Wconf:src=routes/.*:silent",
      "-Wconf:msg=set repeatedly:silent"
    )

  )
  .settings(ScoverageSettings())
  .settings(scalafmtOnCompile := true)

lazy val it = project
  .enablePlugins(PlayScala)
  .dependsOn(microservice % "test->test") // the "test->test" allows reusing test code and test dependencies
  .settings(DefaultBuildSettings.itSettings())
  .settings(scalafmtOnCompile := true)
