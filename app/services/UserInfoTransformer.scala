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

package services

import javax.inject.Singleton
import java.time.LocalDate
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.auth.core.retrieve.ItmpAddress
import config.UserInfoFeatureSwitches
import domain._

@Singleton
class UserInfoTransformer {

  def transform(scopes:      Set[String],
                authority:   Option[Authority],
                desUserInfo: Option[DesUserInfo],
                enrolments:  Option[Enrolments],
                userDetails: Option[UserDetails]
               ): UserInfo = {

    def profile = if (scopes.contains("profile"))
      desUserInfo map (u => UserProfile(u.name.givenName, u.name.familyName, u.name.middleName, u.dateOfBirth))
    else None

    def address = if (scopes.contains("address")) {
      val countryName = desUserInfo flatMap { c => c.address.countryName }
      val countryCode = if (UserInfoFeatureSwitches.countryCode.isEnabled) { desUserInfo flatMap { u => u.address.countryCode } }
      else None
      desUserInfo map (u => Address(formattedAddress(u.address), u.address.postCode, countryName, countryCode))

    } else None

    val identifier = if (scopes.contains("openid:gov-uk-identifiers")) authority flatMap (_.nino) else None

    val userEnrolments = if (scopes.contains("openid:hmrc-enrolments")) enrolments else None

    val ggInfo = if (scopes.contains("openid:government-gateway")) {
      formatGGInfo(authority, userDetails)
    } else None

    val email = if (scopes.contains("email")) userDetails flatMap { _.email }
    else None

    val mdtp = if (scopes.contains("openid:mdtp")) userDetails.flatMap(_.mdtpInformation.map(m => Mdtp(m.deviceId, m.sessionId))) else None

    UserInfo(
      profile.flatMap(_.firstName),
      profile.flatMap(_.familyName),
      profile.flatMap(_.middleName),
      address,
      email,
      profile.flatMap(_.birthDate),
      identifier,
      userEnrolments.map(_.enrolments),
      ggInfo,
      mdtp
    )
  }

  private def formattedAddress(desAddress: ItmpAddress) = {
    val countryName = desAddress.countryName
    val addressLine5 = if (UserInfoFeatureSwitches.addressLine5.isEnabled) desAddress.line5 else None
    Seq(desAddress.line1, desAddress.line2, desAddress.line3, desAddress.line4, addressLine5, desAddress.postCode, countryName).flatten.mkString("\n")
  }

  private def formatGGInfo(authority: Option[Authority], userDetails: Option[UserDetails]): Option[GovernmentGatewayDetails] = {
    val affinityGroup = userDetails flatMap { _.affinityGroup }
    val role = userDetails flatMap { _.credentialRole }
    val userName = userDetails flatMap { _.name }
    val credentialRoles = role.map(Seq(_))
    val credId = authority map { _.credId }
    val agentCode = userDetails flatMap { _.agentCode }
    val agentFriendlyName = userDetails flatMap { _.agentFriendlyName }
    val agentId = userDetails flatMap { _.agentId }
    val gatewayInformation = userDetails flatMap { _.gatewayInformation }
    val mdtp = userDetails flatMap { _.mdtpInformation }
    val profileUrl = userDetails flatMap { _.profile }
    val groupProfileUrl = userDetails flatMap { _.groupProfile }

    Some(
      GovernmentGatewayDetails(
        user_id              = credId,
        user_name            = userName,
        roles                = credentialRoles,
        affinity_group       = affinityGroup,
        agent_code           = agentCode,
        agent_id             = agentId,
        agent_friendly_name  = agentFriendlyName,
        gateway_token        = gatewayInformation.flatMap(_.gatewayToken),
        unread_message_count = None,
        profile_uri          = profileUrl,
        group_profile_uri    = groupProfileUrl
      )
    )
  }

  private case class UserProfile(firstName: Option[String], familyName: Option[String], middleName: Option[String], birthDate: Option[LocalDate])
}
