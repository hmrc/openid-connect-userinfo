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

import play.PlayImport._
import play.core.PlayVersion
import sbt._

object Dependencies {

  private val scalaCheckVersion = "1.12.5"

  val testScope: String = "test,it"

  val playWs = ws exclude("org.apache.httpcomponents", "httpclient") exclude("org.apache.httpcomponents", "httpcore")
  val microserviceBootStrap = "uk.gov.hmrc" %% "microservice-bootstrap" % "4.2.1"
  val playAuthorisation = "uk.gov.hmrc" %% "play-authorisation" % "3.1.0"
  val playHealth = "uk.gov.hmrc" %% "play-health" % "1.1.0"
  val playUrlBinders = "uk.gov.hmrc" %% "play-url-binders" % "1.1.0"
  val playConfig = "uk.gov.hmrc" %% "play-config" % "2.1.0"
  val playJsonLogger = "uk.gov.hmrc" %% "play-json-logger" % "2.1.1"
  val domain = "uk.gov.hmrc" %% "domain" % "3.7.0"
  val referenceChecker = "uk.gov.hmrc" %% "reference-checker" % "2.0.0"
  val scalaCheck = "org.scalacheck" %% "scalacheck" % scalaCheckVersion
  val playHmrcApi = "uk.gov.hmrc" %% "play-hmrc-api" % "0.5.0"

  val hmrcTest = "uk.gov.hmrc" %% "hmrctest" % "1.8.0" % testScope
  val scalaTest = "org.scalatest" %% "scalatest" % "2.2.2" % testScope
  val pegDown = "org.pegdown" % "pegdown" % "1.4.2" % testScope
  val playTest = "com.typesafe.play" %% "play-test" % PlayVersion.current % testScope
  val scalaTestPlus = "org.scalatestplus" %% "play" % "1.2.0" % testScope
  val scalaHttp = "org.scalaj" %% "scalaj-http" % "1.1.5"
  val junit = "junit" % "junit" % "4.12" % testScope
  val wireMock = "com.github.tomakehurst" % "wiremock" % "1.48" % testScope exclude("org.apache.httpcomponents", "httpclient") exclude("org.apache.httpcomponents", "httpcore")

  val compileDependencies = Seq(microserviceBootStrap, playAuthorisation, playHealth, playUrlBinders, playConfig, playJsonLogger, domain, referenceChecker, scalaCheck, playHmrcApi)
  val testDependencies = Seq(hmrcTest, scalaTest, pegDown, playTest, scalaHttp, junit, wireMock)

}