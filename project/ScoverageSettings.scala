import sbt.Keys.parallelExecution
import sbt._
import scoverage.ScoverageKeys

object ScoverageSettings {
  def apply() = Seq( // Semicolon-separated list of regexes matching classes to exclude
    ScoverageKeys.coverageExcludedPackages := "<empty>;Reverse.*;.*(config|views.*);.*(AuthService|BuildInfo|Routes).*",
    ScoverageKeys.coverageExcludedFiles := Seq(
      "" +
        "<empty>",
      "Reverse.*",
      ".*models.*",
      ".*repositories.*",
      ".*BuildInfo.*",
      ".*javascript.*",
      ".*Routes.*",
      ".*GuiceInjector",
      ".*DateTimeQueryStringBinder.*", // better covered via wiremock/E2E integration tests
      ".*Test.*"
    ).mkString(";"),
    ScoverageKeys.coverageMinimum := 62,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true,
    parallelExecution in Test := false
  )
}
