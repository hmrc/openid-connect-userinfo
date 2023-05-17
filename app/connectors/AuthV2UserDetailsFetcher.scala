/*
 * Copyright 2023 HM Revenue & Customs
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

package connectors

import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException}
import domain.UserDetails

import scala.concurrent.{ExecutionContext, Future}

trait AuthV2UserDetailsFetcher extends UserDetailsFetcher {
  self: AuthorisedFunctions =>

  def fetchDetails()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[UserDetails]] = {
    authorised()
      .retrieve(
        Retrievals.allUserDetails and Retrievals.mdtpInformation and Retrievals.gatewayInformation and Retrievals.profile and Retrievals.groupProfile
      ) {
        case credentials ~ maybeName ~ birthDate ~ postCode ~ email ~ affinityGroup ~ agentCode ~ agentInformation ~
            credentialRole ~ description ~ groupId ~ mdtp ~ gatewayInformation ~ profile ~ groupProfile =>
          Future.successful(
            Some(
              UserDetails(
                authProviderId     = credentials.map(_.providerId),
                authProviderType   = credentials.map(_.providerType),
                name               = maybeName.flatMap(_.name),
                lastName           = maybeName.flatMap(_.lastName),
                dateOfBirth        = birthDate,
                postCode           = postCode,
                email              = email,
                affinityGroup      = affinityGroup.map(_.toString()),
                agentCode          = agentCode,
                agentFriendlyName  = agentInformation.agentFriendlyName,
                credentialRole     = credentialRole.map(_.toString),
                description        = description,
                groupIdentifier    = groupId,
                agentId            = agentInformation.agentId,
                gatewayInformation = gatewayInformation,
                mdtpInformation    = mdtp,
                profile            = profile,
                groupProfile       = groupProfile
              )
            )
          )
        case _ => Future.successful(None)
      }
      .recover { case e: NotFoundException =>
        None
      }
  }

}
