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

package connectors

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core.{AuthorisedFunctions, Enrolments, PlayAuthConnector}
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import domain.{Authority, DesUserInfo, UserDetails}
import config.AppContext
import uk.gov.hmrc.http.client.HttpClientV2

import scala.concurrent.{ExecutionContext, Future}

abstract class AuthConnector extends PlayAuthConnector with AuthorisedFunctions {
  self: UserDetailsFetcher =>

  val appContext: AppContext
  val httpClient: HttpClientV2
  val serviceUrl: String = appContext.authUrl

  override def authConnector: AuthConnector = this

  def fetchEnrolments()(implicit headerCarrier: HeaderCarrier, ec: ExecutionContext): Future[Option[Enrolments]] = {
    authorised()
      .retrieve(Retrievals.allEnrolments) { enrolments =>
        Future.successful(Some(enrolments))
      }
      .recover { case UpstreamErrorResponse(_, 404, _, _) =>
        None
      }
  }

  def fetchAuthority()(implicit headerCarrier: HeaderCarrier, ec: ExecutionContext): Future[Option[Authority]] = {
    authorised()
      .retrieve(Retrievals.credentials and Retrievals.nino) {
        case Some(credentials) ~ nino => Future.successful(Some(Authority(credentials.providerId, nino)))
        case _                        => Future.successful(None)
      }
      .recover { case UpstreamErrorResponse(_, 404, _, _) =>
        None
      }
  }

  def fetchDesUserInfo()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[DesUserInfo]] = {
    val nothing = Future.successful(None)
    authorised()
      .retrieve(Retrievals.allItmpUserDetails) {
        case name ~ dateOfBirth ~ address =>
          Future.successful(Some(DesUserInfo(name, dateOfBirth, address)))
        case _ => nothing
      }
      .recoverWith { case UpstreamErrorResponse(_, 404, _, _) =>
        nothing
      }
  }

  override def httpClientV2: HttpClientV2 = httpClient

  def fetchDetails()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[UserDetails]] = self.fetchUserDetails()
}

@Singleton
class AuthConnectorV1 @Inject() (val appContext: AppContext, val httpClient: HttpClientV2)(implicit val executionContext: ExecutionContext)
    extends AuthConnector
    with AuthV1UserDetailsFetcher
