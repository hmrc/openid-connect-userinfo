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

package uk.gov.hmrc.openidconnect.userinfo.domain

import org.joda.time.LocalDate

case class Address(formatted: String,
                   postal_code: String,
                   country: String)

case class UserInfo(given_name: String,
                    family_name: String,
                    middle_name: Option[String],
                    address: Address,
                    birthdate: LocalDate,
                    uk_gov_nino: String)
