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

import javax.inject.{Inject, Singleton}

import uk.gov.hmrc.auth.core.retrieve.{Retrievals, ~}
import uk.gov.hmrc.auth.core.{AuthorisedFunctions, Enrolments, PlayAuthConnector}
import uk.gov.hmrc.http.{CorePost, HeaderCarrier, NotFoundException}
import uk.gov.hmrc.openidconnect.userinfo.domain.{Authority, DesUserInfo, UserDetails}
import uk.gov.hmrc.openidconnect.userinfo.config.AppContext

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

abstract class AuthConnector extends PlayAuthConnector with AuthorisedFunctions {
  self: UserDetailsFetcher =>

  val appContext: AppContext
  val http: CorePost
  val serviceUrl : String = appContext.authUrl

  override def authConnector: AuthConnector = this

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

  def fetchUserDetails()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[UserDetails]] = self.fetchDetails()

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

@Singleton
class AuthConnectorV1 @Inject() (val appContext: AppContext, val http: CorePost) extends AuthConnector with AuthV1UserDetailsFetcher

@Singleton
class AuthConnectorV2 @Inject() (val appContext: AppContext, val http: CorePost) extends AuthConnector with AuthV2UserDetailsFetcher