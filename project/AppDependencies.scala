import play.core.PlayVersion
import play.sbt.PlayImport.*
import sbt.*

object AppDependencies {

  val testScope = "test, it"

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc"       %% "bootstrap-backend-play-28"  % "7.1.0",
    "uk.gov.hmrc"       %% "domain"                     % "8.1.0-play-28",
    "uk.gov.hmrc"       %% "play-hmrc-api"              % "7.2.0-play-28"
  )

  val test: Seq[ModuleID] = Seq(
    "org.scalatest"          %% "scalatest"                % "3.2.9"             % testScope,
    "org.scalatestplus"      %% "scalacheck-1-15"          % "3.2.9.0"           % testScope,
    "org.scalatestplus.play" %% "scalatestplus-play"       % "5.1.0"             % testScope,
    "com.typesafe.play"      %% "play-test"                % PlayVersion.current % testScope,
    "com.github.tomakehurst"  % "wiremock-jre8"            % "2.31.0"            % testScope,
    "uk.gov.hmrc"            %% "service-integration-test" % "1.3.0-play-28"     % testScope,
    "org.pegdown"             % "pegdown"                  % "1.6.0"             % testScope,
    "org.jsoup"               % "jsoup"                    % "1.15.3"            % testScope,
    "org.scalaj"             %% "scalaj-http"              % "2.4.2"             % testScope,
    "org.mockito"            %% "mockito-scala-scalatest"  % "1.17.12"           % testScope,
    "org.scalacheck"         %% "scalacheck"               % "1.16.0"            % testScope,
    "com.github.fge"          % "json-schema-validator"    % "2.2.6"             % testScope,
    "com.vladsch.flexmark"    %  "flexmark-all"            % "0.36.8"            % testScope
  )

  def apply(): Seq[ModuleID] = compile ++ test

}
