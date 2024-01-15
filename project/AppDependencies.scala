import play.core.PlayVersion
import play.sbt.PlayImport.*
import sbt.*

object AppDependencies {

  private val bootstrapPlayVersion = "8.4.0"

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc" %% "bootstrap-backend-play-28" % bootstrapPlayVersion,
    "uk.gov.hmrc" %% "domain"                    % "8.3.0-play-28",
    "uk.gov.hmrc" %% "play-hmrc-api"             % "7.2.0-play-28"
  )

  val test: Seq[ModuleID] = Seq(
    "org.scalatest"          %% "scalatest"                % "3.2.15",
    "org.scalatestplus"      %% "scalacheck-1-15"          % "3.2.11.0",
    "org.scalatestplus.play" %% "scalatestplus-play"       % "5.1.0",
    "com.typesafe.play"      %% "play-test"                % PlayVersion.current,
    "com.github.tomakehurst"  % "wiremock"                 % "2.27.2",
    "uk.gov.hmrc"            %% "bootstrap-test-play-28"   % bootstrapPlayVersion,
    "org.pegdown"             % "pegdown"                  % "1.6.0",
    "org.jsoup"               % "jsoup"                    % "1.15.4",
    "org.scalaj"             %% "scalaj-http"              % "2.4.2",
    "org.mockito"            %% "mockito-scala-scalatest"  % "1.17.12",
    "org.scalacheck"         %% "scalacheck"               % "1.17.0",
    "com.github.fge"          % "json-schema-validator"    % "2.2.6",
    "com.vladsch.flexmark"    % "flexmark-all"             % "0.64.0"
  ).map(_ % Test)

  def apply(): Seq[ModuleID] = compile ++ test

}
