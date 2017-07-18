/*
 * Copyright 2017 HM Revenue & Customs
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

package uk.gov.hmrc.openidconnect.userinfo.config

case class FeatureSwitch(name: String, isEnabled: Boolean)


object FeatureSwitch {

  def forName(name: String) = FeatureSwitch(name, java.lang.Boolean.getBoolean(systemPropertyName(name)))

  def enable(switch: FeatureSwitch): FeatureSwitch = setProp(switch.name, true)

  def disable(switch: FeatureSwitch): FeatureSwitch = setProp(switch.name, false)

  private def setProp(name: String, value: Boolean): FeatureSwitch = {
    sys.props += ((systemPropertyName(name), value.toString))
    forName(name)
  }

  private def systemPropertyName(name: String) = s"feature.$name"

}

object UserInfoFeatureSwitches {

  def countryCode = FeatureSwitch.forName("countryCode")
  def addressLine5 = FeatureSwitch.forName("addressLine5")

}
