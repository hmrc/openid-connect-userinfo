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

import play.api.libs.json._
import uk.gov.hmrc.auth.core.retrieve.{GatewayInformation, ItmpAddress, ItmpName, MdtpInformation}
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier}

package object domain {

  implicit val desUserName = Json.format[ItmpName]

  implicit val desAddress = Json.format[ItmpAddress]

  implicit val enrolmentIdentifier = Json.format[EnrolmentIdentifier]
  implicit val enrloment = Json.format[Enrolment]

  implicit val gatewayInformationFmt: Format[GatewayInformation] = Json.format[GatewayInformation]
  implicit val mdtpInformationFmt:    Format[MdtpInformation] = Json.format[MdtpInformation]
  implicit val userDetails = Json.format[UserDetails]
  implicit val mdtp = Json.format[Mdtp]
  implicit val governmentGatewayDetails = Json.format[GovernmentGatewayDetails]

  implicit val addressFmt = Json.format[Address]
  implicit val userInfoFmt = Json.format[UserInfo]
  implicit val apiAccessFmt = Json.format[APIAccess]

}
