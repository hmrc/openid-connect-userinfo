import play.sbt.PlayImport.PlayKeys.playDefaultPort
import uk.gov.hmrc.DefaultBuildSettings.integrationTestSettings

val appName = "openid-connect-userinfo"

ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always // it should not be needed but the build still fails without it

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin) // Required to prevent https://github.com/scalatest/scalatest/issues/1427
  .settings(scalaVersion := "2.13.8")
  .settings(scalacOptions := Seq("-Xfatal-warnings", "-feature", "-deprecation"))
  .settings(
    majorVersion := 0,
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test
  )
  .configs(IntegrationTest)
  .settings(integrationTestSettings() *)
  .settings(IntegrationTest / dependencyClasspath ++= (Test / exportedProducts).value)
  .settings(IntegrationTest / unmanagedResourceDirectories += baseDirectory.value / "it" / "resources")
  .settings(resolvers += Resolver.jcenterRepo)
  .settings(playDefaultPort := 9836)
  .settings(ScoverageSettings())
  .settings(SilencerSettings())
  .settings(scalafmtOnCompile := true)
