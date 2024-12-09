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

package domain

import play.api.libs.json.{Format, Json, OFormat, Reads}

import java.time.LocalDate
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier}
import uk.gov.hmrc.auth.core.retrieve.{ItmpAddress, ItmpName}

case class Address(formatted: String, postal_code: Option[String], country: Option[String], country_code: Option[String])

object Address {
  implicit val format: OFormat[Address] = Json.format[Address]
}

case class Mdtp(device_id: String, session_id: String)

object Mdtp {
  implicit val format: OFormat[Mdtp] = Json.format[Mdtp]
}

case class UserInfo(given_name: Option[String] = None,
                    family_name: Option[String] = None,
                    middle_name: Option[String] = None,
                    address: Option[Address] = None,
                    email: Option[String] = None,
                    birthdate: Option[LocalDate] = None,
                    uk_gov_nino: Option[String] = None,
                    hmrc_enrolments: Option[Set[Enrolment]] = None,
                    government_gateway: Option[GovernmentGatewayDetails] = None,
                    mdtp: Option[Mdtp] = None,
                    profile_url: Option[String] = None,
                    group_profile_url: Option[String] = None
                   )

object UserInfo {
  /* We re-define our own simple Format for Enrolment because the one provided but the library one
     renames a field ("key" to "enrolment") in a way which disagrees with the UserInfo schema  */
  private implicit val enrFormat: OFormat[Enrolment] = {
    implicit val idFormat: Format[EnrolmentIdentifier] = Json.format[EnrolmentIdentifier]
    Json.format[Enrolment]
  }
  implicit val format: OFormat[UserInfo] = Json.format[UserInfo]
}

case class UserInformation(profile: Option[UserProfile], address: Option[Address], uk_gov_nino: Option[String])

case class UserProfile(given_name: Option[String], family_name: Option[String], middle_name: Option[String], birthdate: Option[LocalDate])

case class DesUserInfo(name: Option[ItmpName], dateOfBirth: Option[LocalDate], address: Option[ItmpAddress])

object DesUserInfo {
  private implicit val inFormat: Format[ItmpName] = Json.format[ItmpName]
  private implicit val iaFormat: Format[ItmpAddress] = Json.format[ItmpAddress]
  implicit val reads: Format[DesUserInfo] = Json.format[DesUserInfo]
}
