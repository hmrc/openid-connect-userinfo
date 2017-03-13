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
import uk.gov.hmrc.openidconnect.userinfo.connectors.ThirdPartyDelegatedAuthorityConnector
import uk.gov.hmrc.openidconnect.userinfo.domain.{Address, DesAddress, DesUserInfo, Enrolment, UserInfo}
import uk.gov.hmrc.play.http.logging.Authorization
import uk.gov.hmrc.play.http.{HeaderCarrier, UnauthorizedException}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

trait UserInfoTransformer {

  val countryService: CountryService
  val thirdPartyDelegatedAuthorityConnector: ThirdPartyDelegatedAuthorityConnector

  def transform(desUserInfo: Option[DesUserInfo], nino: String, enrolments: Option[Seq[Enrolment]])(implicit hc:HeaderCarrier): Future[UserInfo] = {
    def bearerToken(authorization: Authorization) = authorization.value.stripPrefix("Bearer ")

    hc.authorization match {
      case Some(authorization) =>  thirdPartyDelegatedAuthorityConnector.fetchScopes(bearerToken(authorization)) map { scopes =>
        constructUserInfo(desUserInfo, nino, scopes, enrolments)
      }
      case None => Future.failed(new UnauthorizedException("Bearer token is required"))
    }
  }

  private def constructUserInfo(desUserInfo: Option[DesUserInfo], nino: String, scopes: Set[String], enrolments: Option[Seq[Enrolment]]): UserInfo = {
    val userProfile = desUserInfo map (u => UserProfile(u.name.firstForenameOrInitial, u.name.surname, u.name.secondForenameOrInitial, u.dateOfBirth))
    val country = desUserInfo flatMap (u => u.address.countryCode flatMap countryService.getCountry)

    val profile = if (scopes.contains("profile")) userProfile else None
    val identifier = if (scopes.contains("openid:gov-uk-identifiers")) Some(nino) else None
    val address = if (scopes.contains("address")) desUserInfo map (u => Address(formattedAddress(u.address, country), u.address.postcode, country)) else None
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
    Seq(desAddress.line1,desAddress.line2, desAddress.line3, desAddress.line4, desAddress.postcode, country).flatten.mkString("\n")
  }

  private case class UserProfile(firstName: Option[String], familyName: Option[String], middleName: Option[String], birthDate: Option[LocalDate])
}

object UserInfoTransformer extends UserInfoTransformer {
  override val countryService = CountryService
  override val thirdPartyDelegatedAuthorityConnector = ThirdPartyDelegatedAuthorityConnector
}
