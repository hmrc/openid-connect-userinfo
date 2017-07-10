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
import uk.gov.hmrc.openidconnect.userinfo.domain.{Address, Authority, Country, DesAddress, DesUserInfo, Enrolment, GovernmentGatewayDetails, UserDetails, UserInfo}
import uk.gov.hmrc.play.http.Token

trait UserInfoTransformer {

  val countryService: CountryService

  def transform(scopes: Set[String], desUserInfo: Option[DesUserInfo], enrolments: Option[Seq[Enrolment]], authority: Option[Authority], userDetails: Option[UserDetails], token: Option[Token]): UserInfo = {

    def profile = if (scopes.contains("profile")) desUserInfo map (u => UserProfile(u.name.firstForenameOrInitial, u.name.surname, u.name.secondForenameOrInitial, u.dateOfBirth)) else None

    def address = if (scopes.contains("address")) {
      val country = desUserInfo flatMap (u => u.address.countryCode flatMap countryService.getCountry)
      val countryName = country flatMap {c => c.shortName}
      val countryCode = country flatMap {c => c.alphaTwoCode}
      desUserInfo map (u => Address(formattedAddress(u.address, country), u.address.postcode, countryName, countryCode))
    } else None

    val identifier = if (scopes.contains("openid:gov-uk-identifiers")) authority flatMap  {a => a.nino map {n => n}} else None

    val userEnrolments = if (scopes.contains("openid:hmrc_enrolments")) enrolments else None

    val ggInfo = if (scopes.contains("openid:government_gateway")) {
      formatGGInfo(authority, userDetails, token)
    } else None

    val email = if (scopes.contains("email")) userDetails flatMap {_.email} else None

    UserInfo(profile.flatMap(_.firstName),
      profile.flatMap(_.familyName),
      profile.flatMap(_.middleName),
      address,
      email,
      profile.flatMap(_.birthDate),
      identifier,
      userEnrolments,
      ggInfo)
  }

  private def formattedAddress(desAddress: DesAddress, country: Option[Country]) = {
    val countryName = country flatMap {c => c.shortName}
    val countryCode = country flatMap {c => c.alphaTwoCode}
    Seq(desAddress.line1, desAddress.line2, desAddress.line3, desAddress.line4, desAddress.line5, desAddress.postcode, countryName, countryCode).flatten.mkString("\n")
  }

  private def formatGGInfo(authority: Option[Authority], userDetails: Option[UserDetails], token: Option[Token]): Option[GovernmentGatewayDetails] = {
    val (credentialRole, affinityGroup) = (userDetails flatMap {_.credentialRole}, userDetails flatMap {_.affinityGroup})
    val credId = authority flatMap {_.credId}

    Some(GovernmentGatewayDetails(credId, token, credentialRole, affinityGroup))
  }

  private case class UserProfile(firstName: Option[String], familyName: Option[String], middleName: Option[String], birthDate: Option[LocalDate])

}

object UserInfoTransformer extends UserInfoTransformer {
  override val countryService = CountryService
}
