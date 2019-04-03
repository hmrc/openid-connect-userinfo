/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.openidconnect.userinfo.connectors

import uk.gov.hmrc.auth.core.retrieve.{Retrievals, ~}
import uk.gov.hmrc.auth.core.{AuthorisedFunctions, Enrolments, PlayAuthConnector}
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException}
import uk.gov.hmrc.openidconnect.userinfo.config.WSHttp
import uk.gov.hmrc.openidconnect.userinfo.domain.{Authority, DesUserInfo, UserDetails}
import uk.gov.hmrc.play.config.ServicesConfig

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait AuthConnector extends AuthorisedFunctions {
  def fetchEnrolments()(implicit headerCarrier: HeaderCarrier): Future[Option[Enrolments]] = {
    authorised().retrieve(Retrievals.allEnrolments) {
      enrolments => Future.successful(Some(enrolments))
    }.recover {
      case e: NotFoundException => None
    }
  }

  def fetchAuthority()(implicit headerCarrier: HeaderCarrier): Future[Option[Authority]] = {
    authorised().retrieve(Retrievals.credentials and Retrievals.nino) {
      case credentials ~ nino => Future.successful(Some(Authority(credentials.providerId, nino)))
      case _ => Future.successful(None)
    }.recover {
      case e: NotFoundException => None
    }
  }

  def fetchUserDetails()(implicit hc: HeaderCarrier): Future[Option[UserDetails]] = {
    authorised().retrieve(Retrievals.allUserDetails and Retrievals.mdtpInformation and Retrievals.gatewayInformation) {
      case credentials ~ name ~ birthDate ~ postCode ~ email ~ affinityGroup ~ agentCode ~ agentInformation ~
        credentialRole ~ description ~ groupId ~ unreadMessageCount ~ mdtp ~ gatewayInformation =>
        Future.successful(Some(UserDetails(authProviderId = Some(credentials.providerId), authProviderType = Some(credentials.providerType),
          name = name.name, lastName = name.lastName, dateOfBirth = birthDate, postCode = postCode, email = email,
          affinityGroup = affinityGroup.map(_.toString()), agentCode = agentCode,
          agentFriendlyName = agentInformation.agentFriendlyName, credentialRole = credentialRole.map(_.toString),
          description = description, groupIdentifier = groupId, agentId = agentInformation.agentId,
          gatewayInformation = gatewayInformation, mdtpInformation = mdtp, unreadMessageCount = unreadMessageCount)))
      case _ => Future.successful(None)
    }.recover {
      case e: NotFoundException => None
    }
  }

  def fetchDesUserInfo()(implicit hc: HeaderCarrier): Future[Option[DesUserInfo]] = {
    val nothing = Future.successful(None)
    authorised().retrieve(Retrievals.allItmpUserDetails) {
      case name ~ dateOfBirth ~ address =>
        Future.successful(Some(DesUserInfo(name, dateOfBirth, address)))
      case _ => nothing
    }.recoverWith {
      case ex: NotFoundException => nothing
    }
  }
}

object AuthConnector extends AuthConnector with ServicesConfig {
  override def authConnector: uk.gov.hmrc.auth.core.AuthConnector = ConcreteAuthConnector
}

object ConcreteAuthConnector extends PlayAuthConnector with ServicesConfig {
  override val serviceUrl = baseUrl("auth")
  override def http = WSHttp
}