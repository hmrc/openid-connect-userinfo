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

package uk.gov.hmrc.openidconnect.userinfo.domain

import org.joda.time.LocalDate

case class Address(formatted: String,
                   postal_code: Option[String],
                   country: Option[String])

case class UserInfo(given_name: Option[String],
                    family_name: Option[String],
                    middle_name: Option[String],
                    address: Option[Address],
                    birthdate: Option[LocalDate],
                    uk_gov_nino: Option[String])

case class UserInformation(profile: Option[UserProfile],
                           address: Option[Address],
                           uk_gov_nino: Option[String])

case class UserProfile(given_name: Option[String],
                       family_name: Option[String],
                       middle_name: Option[String],
                       birthdate: Option[LocalDate])

case class DesUserInfo(name: DesUserName,
                       dateOfBirth: Option[LocalDate],
                       address: DesAddress)

case class DesUserName(firstForenameOrInitial: Option[String],
                       secondForenameOrInitial: Option[String],
                       surname: Option[String])

case class DesAddress(line1: Option[String],
                      line2: Option[String],
                      line3: Option[String],
                      line4: Option[String],
                      postcode: Option[String],
                      countryCode: Option[Int])
