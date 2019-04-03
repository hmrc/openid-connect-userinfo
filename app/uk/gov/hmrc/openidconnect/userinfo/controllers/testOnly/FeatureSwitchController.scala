/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.openidconnect.userinfo.controllers.testOnly

import play.api.libs.json.Json
import play.api.mvc.Action
import uk.gov.hmrc.openidconnect.userinfo.config.{FeatureSwitch, UserInfoFeatureSwitches}
import uk.gov.hmrc.play.microservice.controller.BaseController

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


trait FeatureSwitchController extends BaseController {

  implicit val featureSwitchReads = Json.reads[FeatureSwitch]
  implicit val featureSwitchWrites = Json.writes[FeatureSwitch]
  implicit val FeatureSwitchRequestReads = Json.reads[FeatureSwitchRequest]

  def getFlags = {
    Action.async { implicit request =>
      Future(Ok(currentFeatureSwitchesAsJson))
    }
  }

  def setFlags = {
    Action.async(parse.json) { implicit request =>
      withJsonBody[FeatureSwitchRequest] { ffRequest =>
        val featureSwitches: Seq[FeatureSwitch] = ffRequest.featureSwitches
        featureSwitches.foreach(fs =>
          fs.isEnabled match {
            case true => FeatureSwitch.enable(FeatureSwitch.forName(fs.name))
            case _ => FeatureSwitch.disable(FeatureSwitch.forName(fs.name))
          }
        )
        Future(Accepted(currentFeatureSwitchesAsJson))
      }
    }
  }

  def currentFeatureSwitchesAsJson = Json.toJson(for (fs <- UserInfoFeatureSwitches.allSwitches) yield FeatureSwitch(fs.name, fs.isEnabled))
}

object FeatureSwitchController extends FeatureSwitchController

case class FeatureSwitchRequest(featureSwitches: Seq[FeatureSwitch]) {}

