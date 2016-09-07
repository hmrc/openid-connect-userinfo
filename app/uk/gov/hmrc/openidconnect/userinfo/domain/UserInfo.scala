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
                   postal_code: Option[String],
                   country: Option[String])

case class UserInfo(given_name: String,
                    family_name: String,
                    middle_name: Option[String],
                    address: Address,
                    birthdate: Option[LocalDate],
                    uk_gov_nino: String)


object UserInfo {

  val addLine = PartialFunction[Option[String], String](_.map(s => s"\n$s").getOrElse(""))

  private def formattedAddress(desAddress: DesAddress, country: Option[String]) = {
    s"${desAddress.line1}\n${desAddress.line2}${addLine(desAddress.line3)}${addLine(desAddress.line4)}${addLine(desAddress.postcode)}${addLine(country)}"
  }

  def from(desUserInfo: DesUserInfo, country: Option[String]): UserInfo = {
    UserInfo(desUserInfo.name.firstForenameOrInitial,
      desUserInfo.name.surname,
      desUserInfo.name.secondForenameOrInitial,
      Address(formattedAddress(desUserInfo.address, country), desUserInfo.address.postcode, country),
      desUserInfo.dateOfBirth,
      desUserInfo.nino)
  }
}

case class DesUserInfo(nino: String,
                       name: DesUserName,
                       dateOfBirth: Option[LocalDate],
                       address: DesAddress)


case class DesUserName(firstForenameOrInitial: String,
                       secondForenameOrInitial: Option[String],
                       surname: String)

case class DesAddress(line1: String,
                      line2: String,
                      line3: Option[String],
                      line4: Option[String],
                      postcode: Option[String],
                      countryCode: Option[Int])
