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

package uk.gov.hmrc.openidconnect.userinfo

import org.joda.time.LocalDate
import play.api.libs.json._
import play.api.libs.functional.syntax._

package object domain {
  implicit val dateReads = Reads.jodaDateReads("yyyy-MM-dd")
  implicit val dateWrites = Writes.jodaDateWrites("yyyy-MM-dd")
  implicit val addressFmt = Json.format[Address]
  implicit val userInfoFmt = Json.format[UserInfo]
  implicit val apiAccessFmt = Json.format[APIAccess]

  implicit val desUserName : Reads[DesUserName] = (
    (JsPath \ "firstForenameOrInitial").readNullable[String] and
      (JsPath \ "secondForenameOrInitial").readNullable[String] and
      (JsPath \ "surname").read[String]
    )(DesUserName.apply _)

  implicit val desAddress : Reads[DesAddress] = (
    (JsPath \ "line1").read[String] and
      (JsPath \ "line2").read[String] and
      (JsPath \ "line3").readNullable[String] and
      (JsPath \ "line4").readNullable[String] and
      (JsPath \ "postcode").readNullable[String] and
      (JsPath \ "countryCode").readNullable[Int]
    )(DesAddress.apply _)

  implicit val desUserInfo : Reads[DesUserInfo] = (
    (JsPath \ "names" \ "1").read[DesUserName] and
      (JsPath \ "dateOfBirth").readNullable[LocalDate] and
      (JsPath \ "addresses" \ "1").read[DesAddress]
    )(DesUserInfo.apply _)

}
