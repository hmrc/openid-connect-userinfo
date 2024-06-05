import play.core.PlayVersion
import play.sbt.PlayImport.*
import sbt.*

object AppDependencies {

  private val bootstrapPlayVersion = "8.6.0"

  private val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc" %% "bootstrap-backend-play-30" % bootstrapPlayVersion,
    "uk.gov.hmrc" %% "domain-play-30"                    % "9.0.0",
    "uk.gov.hmrc" %% "play-hmrc-api-play-30"             % "8.0.0"
  )

  private val test: Seq[ModuleID] = Seq(
    "org.scalaj"                 %% "scalaj-http"             % "2.4.2"    % Test,
    "com.github.java-json-tools" % "json-schema-validator"    % "2.2.14"   % Test,
    "uk.gov.hmrc"                %% "bootstrap-test-play-30"  % "8.6.0"    % Test
  )

  def apply(): Seq[ModuleID] = compile ++ test

}
