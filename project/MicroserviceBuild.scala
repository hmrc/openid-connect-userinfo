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

  private val scalaCheckVersion = "1.12.5"

  val testScope: String = "test,it"

  val playWs = ws exclude("org.apache.httpcomponents", "httpclient") exclude("org.apache.httpcomponents", "httpcore")
  val microserviceBootStrap = "uk.gov.hmrc" %% "microservice-bootstrap" % "4.2.1"
  val playAuthorisation = "uk.gov.hmrc" %% "play-authorisation" % "3.1.0"
  val playHealth = "uk.gov.hmrc" %% "play-health" % "1.1.0"
  val playUrlBinders = "uk.gov.hmrc" %% "play-url-binders" % "1.1.0"
  val playConfig = "uk.gov.hmrc" %% "play-config" % "2.1.0"
  val playJsonLogger = "uk.gov.hmrc" %% "play-json-logger" % "2.1.1"
  val domain = "uk.gov.hmrc" %% "domain" % "3.7.0"
  val referenceChecker = "uk.gov.hmrc" %% "reference-checker" % "2.0.0"
  val scalaCheck = "org.scalacheck" %% "scalacheck" % scalaCheckVersion
  val playHmrcApi = "uk.gov.hmrc" %% "play-hmrc-api" % "0.5.0"

  val hmrcTest = "uk.gov.hmrc" %% "hmrctest" % "1.8.0" % testScope
  val scalaTest = "org.scalatest" %% "scalatest" % "2.2.2" % testScope
  val pegDown = "org.pegdown" % "pegdown" % "1.4.2" % testScope
  val playTest = "com.typesafe.play" %% "play-test" % PlayVersion.current % testScope
  val scalaTestPlus = "org.scalatestplus" %% "play" % "1.2.0" % testScope
  val scalaHttp = "org.scalaj" %% "scalaj-http" % "1.1.5"
  val junit = "junit" % "junit" % "4.12" % testScope
  val wireMock = "com.github.tomakehurst" % "wiremock" % "1.54" % testScope exclude("org.apache.httpcomponents", "httpclient") exclude("org.apache.httpcomponents", "httpcore")

  val compileDependencies = Seq(microserviceBootStrap, playAuthorisation, playHealth, playUrlBinders, playConfig, playJsonLogger, domain, referenceChecker, scalaCheck, playHmrcApi)
  val testDependencies = Seq(hmrcTest, scalaTest, pegDown, playTest, scalaTestPlus, scalaHttp, junit, wireMock)

  def apply() = compileDependencies ++ testDependencies
}
