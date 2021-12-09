import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt._

object AppDependencies {

  val testScope = "test, it"

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc"       %% "bootstrap-backend-play-28"  % "5.17.0",
    "uk.gov.hmrc"       %% "domain"            % "6.2.0-play-28",
    "uk.gov.hmrc"       %% "play-hmrc-api"     % "6.4.0-play-28",
    "com.typesafe.play" %% "play-json-joda"    % "2.7.4"
  )

  val test: Seq[ModuleID] = Seq(
    "org.scalatest"          %% "scalatest"                % "3.2.7"             % testScope,
    "org.scalatestplus"      %% "scalacheck-1-15"          % "3.2.7.0"           % testScope,
    "org.scalatestplus.play" %% "scalatestplus-play"       % "5.1.0"             % testScope,
    "com.typesafe.play"      %% "play-test"                % PlayVersion.current % testScope,
    "com.github.tomakehurst"  % "wiremock-jre8"            % "2.24.1"            % testScope,
    "uk.gov.hmrc"            %% "service-integration-test" % "1.2.0-play-28"     % testScope,
    "org.scoverage"          %% "scalac-scoverage-plugin"  % "1.3.1"             % testScope,
    "org.pegdown"             % "pegdown"                  % "1.6.0"             % testScope,
    "org.jsoup"               % "jsoup"                    % "1.10.3"            % testScope,
    "org.scalaj"             %% "scalaj-http"              % "2.3.0"             % testScope,
    "org.mockito"            %% "mockito-scala-scalatest"  % "1.16.37"           % testScope,
    "org.scalacheck"         %% "scalacheck"               % "1.14.0",
    "com.github.fge"          % "json-schema-validator"    % "2.2.6"             % testScope,
    "com.vladsch.flexmark"    %  "flexmark-all"            % "0.35.10"           % testScope
  )

  def apply(): Seq[ModuleID] = compile ++ test

}
