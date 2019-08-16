import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt._

object MicroServiceBuild extends Build with MicroService {

  val appName = "openid-connect-userinfo"
  override lazy val appDependencies: Seq[ModuleID] = AppDependencies()

}

private object AppDependencies {
  val testScope = "test, it"

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc" %% "bootstrap-play-25" % "4.14.0",
    "org.scalacheck" %% "scalacheck" % "1.13.5",
    "uk.gov.hmrc" %% "play-hmrc-api" % "3.4.0-play-25",
    "uk.gov.hmrc" %% "domain" % "5.6.0-play-25",
    "uk.gov.hmrc" %% "auth-client" % "2.27.0-play-25"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc" %% "service-integration-test" % "0.9.0-play-25" % testScope,
    "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.1" % testScope,
    "org.scoverage" %% "scalac-scoverage-plugin" % "1.3.1" % testScope,
    "org.pegdown" % "pegdown" % "1.6.0" % testScope,
    "org.jsoup" % "jsoup" % "1.10.2" % testScope,
    "com.typesafe.play" %% "play-test" % PlayVersion.current % testScope,
    "com.github.tomakehurst" % "wiremock" % "1.58" % testScope,
    "org.mockito" % "mockito-all" % "1.10.19" % testScope,
    "org.scalaj" %% "scalaj-http" % "2.3.0" % testScope,
    "com.github.fge" % "json-schema-validator" % "2.2.6" % testScope
  )

  def apply(): Seq[ModuleID] = compile ++ test
  
}
