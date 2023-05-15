/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers.testOnly

import config.{FeatureSwitch, UserInfoFeatureSwitches}
import play.api.libs.json.{JsValue, Json, OWrites, Reads}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FeatureSwitchController @Inject() ()(implicit cc: ControllerComponents, ec: ExecutionContext) extends BackendController(cc) {

  implicit val featureSwitchReads:        Reads[FeatureSwitch] = Json.reads[FeatureSwitch]
  implicit val featureSwitchWrites:       OWrites[FeatureSwitch] = Json.writes[FeatureSwitch]
  implicit val FeatureSwitchRequestReads: Reads[FeatureSwitchRequest] = Json.reads[FeatureSwitchRequest]

  def getFlags: Action[AnyContent] = {
    Action { _ =>
      Ok(currentFeatureSwitchesAsJson)
    }
  }

  def setFlags(): Action[JsValue] = {
    Action.async(parse.json) { implicit request =>
      withJsonBody[FeatureSwitchRequest] { ffRequest =>
        val featureSwitches: Seq[FeatureSwitch] = ffRequest.featureSwitches
        featureSwitches.foreach(fs =>
          if (fs.isEnabled) {
            FeatureSwitch.enable(FeatureSwitch.forName(fs.name))
          } else {
            FeatureSwitch.disable(FeatureSwitch.forName(fs.name))
          }
        )
        Future(Accepted(currentFeatureSwitchesAsJson))
      }
    }
  }

  private def currentFeatureSwitchesAsJson = Json.toJson(for (fs <- UserInfoFeatureSwitches.allSwitches) yield FeatureSwitch(fs.name, fs.isEnabled))
}

case class FeatureSwitchRequest(featureSwitches: Seq[FeatureSwitch]) {}
