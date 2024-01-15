import play.sbt.PlayImport.PlayKeys.playDefaultPort
import sbt.Keys.scalacOptions
import uk.gov.hmrc.DefaultBuildSettings

import scala.collection.Seq

val appName = "openid-connect-userinfo"
ThisBuild / majorVersion := 1
ThisBuild / scalaVersion := "2.13.8"
ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always // it should not be needed but the build still fails without it

lazy val microservice = Project(appName, file("."))
  .enablePlugins(PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin) // Required to prevent https://github.com/scalatest/scalatest/issues/1427
  .settings(
    playDefaultPort := 9836,
    libraryDependencies ++= AppDependencies(),
    scalacOptions ++= Seq(
      "-Werror",
      "-feature",
      "-Wconf:cat=unused-imports&src=views/.*:s",
      "-Wconf:src=routes/.*:s"
    )
  )
  .settings(ScoverageSettings())
  .settings(scalafmtOnCompile := true)

lazy val it = project
  .enablePlugins(PlayScala)
  .dependsOn(microservice % "test->test") // the "test->test" allows reusing test code and test dependencies
  .settings(DefaultBuildSettings.itSettings())
  .settings(Test / dependencyClasspath ++= (Test / exportedProducts).value)
  .settings(Test / unmanagedResourceDirectories += baseDirectory.value / "it" / "resources")
  .settings(
    Test / parallelExecution := false,
    Test / fork := false
  )
