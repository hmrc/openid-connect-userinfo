import play.core.PlayVersion
import play.sbt.PlayImport.*
import sbt.*

object AppDependencies {

  private val bootstrapPlayVersion = "8.5.0"

  private val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc" %% "bootstrap-backend-play-30" % bootstrapPlayVersion,
    "uk.gov.hmrc" %% "domain-play-30"                    % "9.0.0",
    "uk.gov.hmrc" %% "play-hmrc-api-play-30"             % "8.0.0"
  )

  private val test: Seq[ModuleID] = Seq(
    "org.scalatest"             %% "scalatest"               % "3.2.17"             % Test,
    "org.scalatestplus"         %% "scalacheck-1-17"         % "3.2.17.0"           % Test,
    "org.scalatestplus.play"    %% "scalatestplus-play"      % "5.1.0"              % Test,
    "org.playframework"         %% "play-test"               % PlayVersion.current  % Test,
    "com.github.tomakehurst"     % "wiremock"                % "2.27.2"             % Test,
    "uk.gov.hmrc"               %% "bootstrap-test-play-30"  % bootstrapPlayVersion % Test,
    "org.pegdown"                % "pegdown"                 % "1.6.0"              % Test,
    "org.jsoup"                  % "jsoup"                   % "1.17.2"             % Test,
    "org.scalaj"                %% "scalaj-http"             % "2.4.2"              % Test,
    "org.mockito"               %% "mockito-scala-scalatest" % "1.17.30"            % Test,
    "org.scalacheck"            %% "scalacheck"              % "1.17.0"             % Test,
    "com.github.java-json-tools" % "json-schema-validator"   % "2.2.14"             % Test,
    "com.vladsch.flexmark"       % "flexmark-all"            % "0.64.0"             % Test
  )

  def apply(): Seq[ModuleID] = compile ++ test

}
