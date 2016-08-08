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

package it.utils

import play.api.Play
import play.api.test.FakeApplication

trait MicroserviceLocalRunSugar {
  val additionalConfiguration: Map[String, Any]
  val localMicroserviceUrl = s"http://localhost:${port}"
  val port = sys.env.getOrElse("MICROSERVICE_PORT", "9001").toInt
  lazy val fakeApplication = FakeApplication(additionalConfiguration = additionalConfiguration)

  def run(block: () => Unit) = {
    Play.start(fakeApplication)
    block()
    Play.stop()
  }
}