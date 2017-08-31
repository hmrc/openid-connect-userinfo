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

package uk.gov.hmrc.openidconnect.userinfo.connectors

import play.api.Logger
import uk.gov.hmrc.auth.core.retrieve.{Retrievals, ~}
import uk.gov.hmrc.auth.core.{AuthorisedFunctions, PlayAuthConnector}
import uk.gov.hmrc.openidconnect.userinfo.config.WSHttp
import uk.gov.hmrc.openidconnect.userinfo.domain.{Authority, DesUserInfo, Enrolment, UserDetails}
import uk.gov.hmrc.play.auth.microservice.connectors.ConfidenceLevel._
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet, NotFoundException}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait AuthConnector extends uk.gov.hmrc.play.auth.microservice.connectors.AuthConnector with AuthorisedFunctions {
  val authBaseUrl: String

  val http: HttpGet

  def fetchEnrolments(auth: Authority)(implicit headerCarrier: HeaderCarrier): Future[Option[Seq[Enrolment]]] = {
    auth.enrolments map { enrolmentsUri =>
      http.GET(s"$authBaseUrl$enrolmentsUri") map { response =>
        response.json.asOpt[Seq[Enrolment]]
      } recover {
        case e: NotFoundException => {
          None
        }
      }
    } getOrElse {
      Logger.debug("No enrolment uri.")
      Future.successful(None)
    }
  }

  def fetchUserDetails(auth: Authority)(implicit hc: HeaderCarrier): Future[Option[UserDetails]] = {
    authorised().retrieve(Retrievals.allUserDetails) {
      case credentials ~ name ~ birthDate ~ postCode ~ email ~ affinityGroup ~ agentCode ~ agentInformation ~
        credentialRole ~ description ~ groupId =>
        Future.successful(Some(UserDetails(authProviderId = Some(credentials.providerId), authProviderType = Some(credentials.providerType),
          name = name.name, lastName = name.lastName, dateOfBirth = birthDate, postCode = postCode, email = email,
          affinityGroup = affinityGroup.map(_.toString()), agentCode = agentCode,
          agentFriendlyName = agentInformation.agentFriendlyName, credentialRole = credentialRole.map(_.toString),
          description = description, groupIdentifier = groupId, agentId = agentInformation.agentId)))
      case _ => Future.successful(None)
    }.recover {
      case e: NotFoundException => None
    }

    def fetchDesUserInfo(authority: Authority)(implicit hc: HeaderCarrier): Future[Option[DesUserInfo]] = {
      val nothing = Future.successful(None)
      if (authority.nino.isDefined)
        authorised().retrieve(Retrievals.allItmpUserDetails) {
          case name ~ dateOfBirth ~ address =>
            Future.successful(Some(DesUserInfo(name, dateOfBirth, address)))
          case _ => nothing
        }.recoverWith {
          case ex: NotFoundException => nothing
        }
      else nothing
    }

    def fetchAuthority()(implicit headerCarrier: HeaderCarrier): Future[Option[Authority]] = {
      http.GET(s"$authBaseUrl/auth/authority") map { response =>
        response.json.asOpt[Authority]
      }
    }
  }

  object AuthConnector extends AuthConnector with ServicesConfig {
    override lazy val authBaseUrl = baseUrl("auth")
    lazy val http = WSHttp

    override def authConnector = new PlayAuthConnector {
      override val serviceUrl = baseUrl("auth")

      override def http = WSHttp
    }
  }
