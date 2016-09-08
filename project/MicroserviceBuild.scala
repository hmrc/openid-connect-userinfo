import play.core.PlayVersion
import sbt._
import uk.gov.hmrc.SbtAutoBuildPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.versioning.SbtGitVersioning
import play.PlayImport._

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
    "uk.gov.hmrc" %% "microservice-bootstrap" % "4.4.0",
    "uk.gov.hmrc" %% "play-authorisation" % "3.3.0",
    "uk.gov.hmrc" %% "play-config" % "2.1.0",
    "uk.gov.hmrc" %% "play-json-logger" % "2.1.1",
    "uk.gov.hmrc" %% "play-health" % "1.1.0",
    "uk.gov.hmrc" %% "play-config" % "2.1.0",
    "org.scalacheck" %% "scalacheck" % "1.12.5",
    "uk.gov.hmrc" %% "play-hmrc-api" % "0.5.0",
    "uk.gov.hmrc" %% "domain" % "3.7.0"
  )

  val test = Seq(
    "uk.gov.hmrc" %% "hmrctest" % "1.8.0" % testScope,
    "org.scalatest" %% "scalatest" % "2.2.6" % testScope,
    "org.scalatestplus" %% "play" % "1.2.0" % testScope,
    "org.pegdown" % "pegdown" % "1.5.0" % testScope,
    "org.jsoup" % "jsoup" % "1.7.3" % testScope,
    "com.typesafe.play" %% "play-test" % PlayVersion.current % testScope,
    "com.github.tomakehurst" % "wiremock" % "1.57" % testScope,
    "org.scalaj" %% "scalaj-http" % "1.1.5" % testScope
  )

  def apply() = compile ++ test
}
