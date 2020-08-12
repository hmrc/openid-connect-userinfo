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
    "org.scalacheck"    %% "scalacheck"        % "1.13.5",
    "uk.gov.hmrc"       %% "bootstrap-play-26" % "1.13.0",
    "uk.gov.hmrc"       %% "play-hmrc-api"     % "4.1.0-play-26",
    "uk.gov.hmrc"       %% "domain"            % "5.9.0-play-26",
    "com.typesafe.play" %% "play-json-joda"    % "2.6.10"
  )

  val test: Seq[ModuleID] = Seq(
    "com.typesafe.play"      %% "play-test"                % PlayVersion.current % testScope,
    "com.github.tomakehurst"  % "wiremock-jre8"            % "2.24.0"            % testScope,
    "uk.gov.hmrc"            %% "service-integration-test" % "0.12.0-play-26"    % testScope,
    "org.scalatestplus.play" %% "scalatestplus-play"       % "3.1.2"             % testScope,
    "org.scoverage"          %% "scalac-scoverage-plugin"  % "1.3.1"             % testScope,
    "org.pegdown"             % "pegdown"                  % "1.6.0"             % testScope,
    "org.jsoup"               % "jsoup"                    % "1.10.2"            % testScope,
    "org.mockito"             % "mockito-all"              % "1.10.19"           % testScope,
    "org.scalaj"             %% "scalaj-http"              % "2.3.0"             % testScope,
    "com.github.fge"          % "json-schema-validator"    % "2.2.6"             % testScope,
    "org.skyscreamer"         % "jsonassert"               % "1.5.0"             % testScope
  )

  def apply(): Seq[ModuleID] = compile ++ test
  
}
