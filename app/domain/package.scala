/*
 * Copyright 2024 HM Revenue & Customs
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

import play.api.libs.json._
import uk.gov.hmrc.auth.core.retrieve.{GatewayInformation, ItmpAddress, ItmpName, MdtpInformation}
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier}

package object domain {

  implicit val desUserName: OFormat[ItmpName] = Json.format

  implicit val desAddress: OFormat[ItmpAddress] = Json.format

  implicit val enrolmentIdentifier: OFormat[EnrolmentIdentifier] = Json.format
  implicit val enrloment:           OFormat[Enrolment] = Json.format

  implicit val gatewayInformationFmt:    Format[GatewayInformation] = Json.format
  implicit val mdtpInformationFmt:       Format[MdtpInformation] = Json.format
  implicit val userDetails:              OFormat[UserDetails] = Json.format
  implicit val mdtp:                     OFormat[Mdtp] = Json.format
  implicit val governmentGatewayDetails: OFormat[GovernmentGatewayDetails] = Json.format

  implicit val addressFmt:   OFormat[Address] = Json.format
  implicit val userInfoFmt:  OFormat[UserInfo] = Json.format
  implicit val apiAccessFmt: OFormat[APIAccess] = Json.format

}
