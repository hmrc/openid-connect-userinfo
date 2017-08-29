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

package uk.gov.hmrc.openidconnect.userinfo

import org.joda.time.LocalDate
import play.api.libs.functional.syntax._
import play.api.libs.json._
import uk.gov.hmrc.auth.core.retrieve.{ItmpAddress, ItmpName}
import uk.gov.hmrc.play.http.Token

package object domain {

  implicit val desUserName = Json.format[ItmpName]

  implicit val desAddress = Json.format[ItmpAddress]

//  implicit val desUserInfo : Reads[DesUserInfo] = (
//  (JsPath \ "names" \ "1").read[ItmpName] and
//  (JsPath \ "dateOfBirth").readNullable[LocalDate] and
//  (JsPath \ "addresses" \ "1").read[ItmpAddress]
//  )(DesUserInfo.apply _)

  implicit val enrolmentIdentifier = Json.format[EnrolmentIdentifier]
  implicit val enrloment = Json.format[Enrolment]

  implicit val token = Json.format[Token]
  implicit val userDetails = Json.format[UserDetails]
  implicit val governmentGatewayDetails = Json.format[GovernmentGatewayDetails]

  implicit val dateReads = Reads.jodaDateReads("yyyy-MM-dd")
  implicit val dateWrites = Writes.jodaDateWrites("yyyy-MM-dd")
  implicit val addressFmt = Json.format[Address]
  implicit val authorityFmt = Json.format[Authority]
  implicit val userInfoFmt = Json.format[UserInfo]
  implicit val apiAccessFmt = Json.format[APIAccess]
}
