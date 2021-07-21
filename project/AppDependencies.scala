import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt._

object AppDependencies {

  val testScope = "test, it"

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc"       %% "bootstrap-backend-play-27"  % "5.7.0",
    "uk.gov.hmrc"       %% "domain"            % "6.1.0-play-27",
    "uk.gov.hmrc"       %% "play-hmrc-api"     % "6.4.0-play-27",
    "com.typesafe.play" %% "play-json-joda"    % "2.7.4"
  )

  val test: Seq[ModuleID] = Seq(
    "com.typesafe.play"      %% "play-test"                % PlayVersion.current % testScope,
    "org.scalacheck"         %% "scalacheck"               % "1.13.5",
    "com.github.tomakehurst"  % "wiremock-jre8"            % "2.24.1"            % testScope,
    "uk.gov.hmrc"            %% "service-integration-test" % "1.1.0-play-27"    % testScope,
    "org.scalatestplus.play" %% "scalatestplus-play"       % "3.1.3"             % testScope,
    "org.scoverage"          %% "scalac-scoverage-plugin"  % "1.3.1"             % testScope,
    "org.pegdown"             % "pegdown"                  % "1.6.0"             % testScope,
    "org.jsoup"               % "jsoup"                    % "1.10.3"            % testScope,
    "org.mockito"             % "mockito-all"              % "1.10.19"           % testScope,
    "org.scalaj"             %% "scalaj-http"              % "2.3.0"             % testScope,
    "com.github.fge"          % "json-schema-validator"    % "2.2.6"             % testScope
  )

  def apply(): Seq[ModuleID] = compile ++ test

}
