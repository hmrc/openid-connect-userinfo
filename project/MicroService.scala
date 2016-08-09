import sbt.Keys._
import sbt.Tests.{Group, SubProcess}
import sbt._
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._


trait MicroService {

  import uk.gov.hmrc._
  import DefaultBuildSettings._
  import TestPhases._

  val appName: String

  lazy val appDependencies : Seq[ModuleID] = ???
  lazy val plugins : Seq[Plugins] = Seq(play.PlayScala)
  lazy val playSettings : Seq[Setting[_]] = Seq.empty

  lazy val ItTest = config("it") extend Test

  lazy val microservice = Project(appName, file("."))
    .enablePlugins(Seq(play.PlayScala) ++ plugins : _*)
    .settings(playSettings : _*)
    .settings(scalaSettings: _*)
    .settings(publishingSettings: _*)
    .settings(defaultSettings(): _*)
    .settings(

      targetJvm := "jvm-1.8",
      scalaVersion := "2.11.8",
      // use lint option to avoid missing string interpolation warnings https://github.com/playframework/playframework/issues/5134
      // remove when we upgrade to play 2.5.x https://github.com/playframework/playframework/pull/5135
      scalacOptions ++= Seq("-Xlint:-missing-interpolator"),
      libraryDependencies ++= appDependencies,
      parallelExecution in Test := false,
      fork in Test := false,
      retrieveManaged := true,
      evictionWarningOptions in update := EvictionWarningOptions.default.withWarnScalaVersionEviction(false)
    )
    .configs(ItTest)
    .settings(inConfig(ItTest)(Defaults.testSettings): _*)
    .settings(
      Keys.fork in ItTest := false,
      unmanagedSourceDirectories in ItTest <<= (baseDirectory in ItTest)(base => Seq(base / "func")),
      unmanagedClasspath in ItTest += baseDirectory.value / "resources",
      unmanagedClasspath in Runtime += baseDirectory.value / "resources",
      unmanagedResourceDirectories in Compile += baseDirectory.value / "resources",
      addTestReportOption(ItTest, "int-test-reports"),
      testGrouping in ItTest := oneForkedJvmPerTest((definedTests in ItTest).value),
      parallelExecution in ItTest := false)
    .settings(resolvers += Resolver.bintrayRepo("hmrc", "releases"))
}

private object TestPhases {

  def oneForkedJvmPerTest(tests: Seq[TestDefinition]) =
    tests map {
      test => new Group(test.name, Seq(test), SubProcess(ForkOptions(runJVMOptions = Seq("-Dtest.name=" + test.name))))
    }
}