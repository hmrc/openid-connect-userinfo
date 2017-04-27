import play.core.PlayVersion
import sbt._
import uk.gov.hmrc.SbtAutoBuildPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.versioning.SbtGitVersioning
import play.sbt.PlayImport._

object MicroServiceBuild extends Build with MicroService {

  val appName = "openid-connect-userinfo"

  override lazy val plugins: Seq[Plugins] = Seq(
    SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin
  )

  override lazy val appDependencies: Seq[ModuleID] = AppDependencies()
}

private object AppDependencies {
  val testScope = "test, it"

  val compile = Seq(
    ws,
    "uk.gov.hmrc" %% "microservice-bootstrap" % "5.8.0",
    "uk.gov.hmrc" %% "play-authorisation" % "4.2.0",
    "uk.gov.hmrc" %% "play-config" % "3.0.0",
    "uk.gov.hmrc" %% "logback-json-logger" % "3.1.0",
    "uk.gov.hmrc" %% "play-health" % "2.0.0",
    "org.scalacheck" %% "scalacheck" % "1.12.5",
    "uk.gov.hmrc" %% "play-hmrc-api" % "1.2.0",
    "uk.gov.hmrc" %% "domain" % "4.0.0"
  )

  val test = Seq(
    "uk.gov.hmrc" %% "hmrctest" % "2.2.0" % testScope,
    "org.scalatest" %% "scalatest" % "2.2.6" % testScope,
    "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % testScope,
    "org.pegdown" % "pegdown" % "1.6.0" % testScope,
    "org.jsoup" % "jsoup" % "1.10.1" % testScope,
    "com.typesafe.play" %% "play-test" % PlayVersion.current % testScope,
    "com.github.tomakehurst" % "wiremock" % "1.58" % testScope,
    "org.mockito" % "mockito-all" % "1.10.19" % testScope,
    "org.scalaj" %% "scalaj-http" % "2.3.0" % testScope,
    "com.github.fge" % "json-schema-validator" % "2.2.6" % testScope
  )

  def apply() = compile ++ test
}
