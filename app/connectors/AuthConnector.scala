/*
 * Copyright 2021 HM Revenue & Customs
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

import com.github.ghik.silencer.silent
import uk.gov.hmrc.auth.core.retrieve.{Retrievals, ~}
import uk.gov.hmrc.auth.core.{AuthorisedFunctions, Enrolments, PlayAuthConnector}
import uk.gov.hmrc.http.{CorePost, HeaderCarrier, NotFoundException}
import domain.{Authority, DesUserInfo, UserDetails}
import config.AppContext

import scala.concurrent.{ExecutionContext, Future}

@silent abstract class AuthConnector extends PlayAuthConnector with AuthorisedFunctions {
  self: UserDetailsFetcher =>

  val appContext: AppContext
  val http: CorePost
  val serviceUrl: String = appContext.authUrl

  override def authConnector: AuthConnector = this

  def fetchEnrolments()(implicit headerCarrier: HeaderCarrier, ec: ExecutionContext): Future[Option[Enrolments]] = {
    authorised().retrieve(Retrievals.allEnrolments) {
      enrolments => Future.successful(Some(enrolments))
    }.recover {
      case e: NotFoundException => None
    }
  }

  def fetchAuthority()(implicit headerCarrier: HeaderCarrier, ec: ExecutionContext): Future[Option[Authority]] = {
    authorised().retrieve(Retrievals.credentials and Retrievals.nino) {
      case credentials ~ nino => Future.successful(Some(Authority(credentials.providerId, nino)))
      case _                  => Future.successful(None)
    }.recover {
      case e: NotFoundException => None
    }
  }

  def fetchUserDetails()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[UserDetails]] = self.fetchDetails()(hc, ec)

  def fetchDesUserInfo()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[DesUserInfo]] = {
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

@Singleton
class AuthConnectorV1 @Inject() (val appContext: AppContext, val http: CorePost)(implicit val executionContext: ExecutionContext) extends AuthConnector with AuthV1UserDetailsFetcher

@Singleton
class AuthConnectorV2 @Inject() (val appContext: AppContext, val http: CorePost)(implicit val executionContext: ExecutionContext) extends AuthConnector with AuthV2UserDetailsFetcher
