/*
 * Copyright 2016 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import Dependencies._
import sbt.Keys._
import sbt.Tests.{Group, SubProcess}
import sbt._
import uk.gov.hmrc.DefaultBuildSettings._
import uk.gov.hmrc.NexusPublishing.nexusPublishingSettings
import uk.gov.hmrc.PublishingSettings._
import uk.gov.hmrc.SbtBuildInfo
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._

import scala.util.Properties._

name := "openid-connect-userinfo"
version := envOrElse("USER_INFORMATION_VERSION", "999-SNAPSHOT")
targetJvm := "jvm-1.8"
resolvers += Resolver.bintrayRepo("hmrc", "releases")

lazy val UnitTest = config("unit") extend Test
lazy val ComponentTest = config("component") extend IntegrationTest

val testConfig = Seq(IntegrationTest, UnitTest, ComponentTest)

lazy val microservice = (project in file("."))
  .enablePlugins(PlayScala)
  .disablePlugins(sbt.plugins.JUnitXmlReportPlugin)
  .configs(testConfig: _*)
  .settings(
    commonSettings,
    staticCompileResourceSettings,
    unitTestSettings,
    componentTestSettings,
    itTestSettings,
    testSettings,
    playPublishingSettings
  )

lazy val componentTestSettings =
  inConfig(ComponentTest)(Defaults.testSettings) ++
    Seq(
      testOptions in ComponentTest := Seq(Tests.Filter((name: String) => name startsWith "component")),
      unmanagedSourceDirectories in ComponentTest <<= (baseDirectory in ComponentTest)(base => Seq(base / "test/component"))
    )

lazy val unitTestSettings =
  inConfig(UnitTest)(Defaults.testSettings) ++
    Seq(
      testOptions in UnitTest := Seq(Tests.Filter((name: String) => name startsWith "unit")),
      unmanagedSourceDirectories in UnitTest <<= (baseDirectory in UnitTest)(base => Seq(base / "test/unit"))
    )

lazy val itTestSettings =
  inConfig(IntegrationTest)(Defaults.itSettings) ++
    Seq(
      Keys.fork in IntegrationTest := false,
      unmanagedSourceDirectories in IntegrationTest <<= (baseDirectory in IntegrationTest)(base => Seq(base / "test/it")),
      addTestReportOption(IntegrationTest, "int-test-reports"),
      testGrouping in IntegrationTest := oneForkedJvmPerTest((definedTests in IntegrationTest).value),
      parallelExecution in IntegrationTest := false
    )

lazy val testSettings =
  inConfig(Test)(Defaults.testSettings) ++
    Seq(
      fork in Test := false,
      unmanagedSourceDirectories in Test <<= (baseDirectory in Test)(base => Seq(base / "test")),
      testGrouping in Test := oneForkedJvmPerTest((definedTests in Test).value),
      parallelExecution in Test := false
    )

lazy val commonSettings: Seq[Setting[_]] = scalaSettings ++
  publishingSettings ++
  defaultSettings() ++
  gitStampSettings ++
  SbtBuildInfo()

lazy val staticCompileResourceSettings =
  unmanagedResourceDirectories in Compile += baseDirectory.value / "resources"

def oneForkedJvmPerTest(tests: Seq[TestDefinition]) = tests map {
  test => new Group(test.name, Seq(test), SubProcess(ForkOptions(runJVMOptions = Seq("-Dtest.name=" + test.name))))
}

lazy val playPublishingSettings: Seq[sbt.Setting[_]] = sbtrelease.ReleasePlugin.releaseSettings ++
  Seq(credentials += SbtCredentials) ++
  publishAllArtefacts ++
  nexusPublishingSettings

libraryDependencies ++= compileDependencies ++ testDependencies
