/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.openidconnect.userinfo

import org.joda.time._
import play.api.libs.json._
import uk.gov.hmrc.auth.core.retrieve.{GatewayInformation, ItmpAddress, ItmpName, MdtpInformation}
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier}
import uk.gov.hmrc.http.Token

package object domain {
  implicit val dFormat: Format[LocalDate] = new Format[LocalDate] {
    override def reads(json: JsValue): JsResult[LocalDate] = JodaReads.DefaultJodaLocalDateReads.reads(json)
    override def writes(o: LocalDate): JsValue = JodaWrites.DefaultJodaLocalDateWrites.writes(o)
  }

  implicit val desUserName = Json.format[ItmpName]

  implicit val desAddress = Json.format[ItmpAddress]

  implicit val enrolmentIdentifier = Json.format[EnrolmentIdentifier]
  implicit val enrloment = Json.format[Enrolment]

  implicit val token = Json.format[Token]
  implicit val gatewayInformationFmt: Format[GatewayInformation] = Json.format[GatewayInformation]
  implicit val mdtpInformationFmt: Format[MdtpInformation] = Json.format[MdtpInformation]
  implicit val userDetails = Json.format[UserDetails]
  implicit val mdtp = Json.format[Mdtp]
  implicit val governmentGatewayDetails = Json.format[GovernmentGatewayDetails]

  implicit val dateReads = JodaReads.jodaDateReads("yyyy-MM-dd")
  implicit val dateWrites = JodaWrites.jodaDateWrites("yyyy-MM-dd")
  implicit val addressFmt = Json.format[Address]
  implicit val userInfoFmt = Json.format[UserInfo]
  implicit val apiAccessFmt = Json.format[APIAccess]
}
