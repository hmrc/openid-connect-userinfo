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

package uk.gov.hmrc.openidconnect.userinfo.services

import org.joda.time.LocalDate
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.openidconnect.userinfo.domain.{Address, DesAddress, DesUserInfo, Enrolment, UserInfo}

trait UserInfoTransformer {

  val countryService: CountryService

  def transform(scopes: Set[String], desUserInfo: Option[DesUserInfo], nino: Option[Nino], enrolments: Option[Seq[Enrolment]]): UserInfo = {
    def profile = if (scopes.contains("profile")) desUserInfo map (u => UserProfile(u.name.firstForenameOrInitial, u.name.surname, u.name.secondForenameOrInitial, u.dateOfBirth)) else None

    def address = if (scopes.contains("address")) {
      val country = desUserInfo flatMap (u => u.address.countryCode flatMap countryService.getCountry)
      desUserInfo map (u => Address(formattedAddress(u.address, country), u.address.postcode, country))
    } else None

    val identifier = if (scopes.contains("openid:gov-uk-identifiers")) nino.map(_.nino) else None
    val userEnrolments = if (scopes.contains("openid:hmrc_enrolments")) enrolments else None

    UserInfo(profile.flatMap(_.firstName),
      profile.flatMap(_.familyName),
      profile.flatMap(_.middleName),
      address,
      profile.flatMap(_.birthDate),
      identifier,
      userEnrolments)
  }

  private def formattedAddress(desAddress: DesAddress, country: Option[String]) = {
    Seq(desAddress.line1, desAddress.line2, desAddress.line3, desAddress.line4, desAddress.postcode, country).flatten.mkString("\n")
  }

  private case class UserProfile(firstName: Option[String], familyName: Option[String], middleName: Option[String], birthDate: Option[LocalDate])

}

object UserInfoTransformer extends UserInfoTransformer {
  override val countryService = CountryService
}
