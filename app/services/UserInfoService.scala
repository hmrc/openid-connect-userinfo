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

package services

import javax.inject.{Inject, Named, Singleton}
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.http.Authorization
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, UnauthorizedException}
import connectors._
import controllers.{Version, Version_1_0}
import data.UserInfoGenerator
import domain._

import scala.concurrent.{ExecutionContext, Future}

trait UserInfoService {
  def fetchUserInfo(version: Version)(implicit hc: HeaderCarrier): Future[UserInfo]
}

class LiveUserInfoService @Inject() (
  v1AuthConnector:                       AuthConnector,
  userInfoTransformer:                   UserInfoTransformer,
  thirdPartyDelegatedAuthorityConnector: ThirdPartyDelegatedAuthorityConnector
)(implicit ec: ExecutionContext)
    extends UserInfoService {

  override def fetchUserInfo(version: Version)(implicit hc: HeaderCarrier): Future[UserInfo] = {

    val scopes = hc.authorization match {
      case Some(authorisationTokens) => thirdPartyDelegatedAuthorityConnector.fetchScopes(authorisationTokens.value)
      case None                      => Future.failed(new UnauthorizedException("Authorization token is required"))
    }

    scopes.flatMap { scopes =>
      def getMaybeForScopes[T](maybeScopes: Set[String], allScopes: Set[String], f: => Future[Option[T]]): Future[Option[T]] = {
        if ((maybeScopes intersect allScopes).nonEmpty) f
        else Future.successful(None)
      }

      val scopesForAuthority =
        Set("openid:government-gateway", "email", "profile", "address", "openid:gov-uk-identifiers", "openid:hmrc-enrolments", "openid:mdtp")
      val maybeAuthority = getMaybeForScopes(scopesForAuthority, scopes, v1AuthConnector.fetchAuthority())

      val scopesForUserDetails = Set("openid:government-gateway", "email", "openid:mdtp")
      def maybeUserDetails = getMaybeForScopes[UserDetails](scopesForUserDetails, scopes, v1AuthConnector.fetchUserDetails())

      val scopesForDes = Set("profile", "address")
      def maybeDesUserInfo = {
        getMaybeForScopes[DesUserInfo](
          scopesForDes,
          scopes,
          maybeAuthority flatMap {
            case Some(auth) if auth.nino.isDefined => v1AuthConnector.fetchDesUserInfo()
            case _                                 => Future.failed(new BadRequestException("NINO not found for this user"))
          }
        )
      }

      def maybeEnrolments = getMaybeForScopes[Enrolments](Set("openid:hmrc-enrolments"), scopes, v1AuthConnector.fetchEnrolments())

      for {
        authority   <- maybeAuthority
        enrolments  <- maybeEnrolments
        desUserInfo <- maybeDesUserInfo
        userDetails <- maybeUserDetails
      } yield userInfoTransformer.transform(scopes, authority, desUserInfo, enrolments, userDetails)
    }
  }
}

@Singleton
class SandboxUserInfoService @Inject() (userInfoGenerator: UserInfoGenerator) extends UserInfoService {
  override def fetchUserInfo(version: Version)(implicit hc: HeaderCarrier): Future[UserInfo] = {
    val generator: UserInfo = version match {
      case Version_1_0 => userInfoGenerator.userInfoV1_0()
      case _           => UserInfo()
    }
    Future.successful(generator)
  }
}
