
import play.sbt.PlayImport._
import sbt._

object AppDependencies {

  private val bootstrapPlayVersion = "9.4.0"

  private val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc" %% "bootstrap-backend-play-30" % bootstrapPlayVersion,
    "uk.gov.hmrc" %% "domain-play-30"                    % "10.0.0",
  )

  private val test: Seq[ModuleID] = Seq(
    "com.github.java-json-tools" % "json-schema-validator"    % "2.2.14"   % Test,
    "uk.gov.hmrc"                %% "bootstrap-test-play-30"  % bootstrapPlayVersion    % Test
  )

  def apply(): Seq[ModuleID] = compile ++ test

}
