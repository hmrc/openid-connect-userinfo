/*
 * Copyright 2020 HM Revenue & Customs
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

import javax.inject.{Inject, Named, Singleton}

import org.scalacheck.Gen
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.http.logging.Authorization
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, UnauthorizedException}
import uk.gov.hmrc.openidconnect.userinfo.connectors._
import uk.gov.hmrc.openidconnect.userinfo.controllers.{Version, Version_1_0, Version_1_1}
import uk.gov.hmrc.openidconnect.userinfo.data.UserInfoGenerator
import uk.gov.hmrc.openidconnect.userinfo.domain._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait UserInfoService {
  def fetchUserInfo(version: Version)(implicit hc: HeaderCarrier): Future[UserInfo]
}

class LiveUserInfoService @Inject()
  (
    @Named("v1Connector") v1AuthConnector: AuthConnector,
    @Named("v2Connector") v2AuthConnector: AuthConnector,
    userInfoTransformer: UserInfoTransformer,
    thirdPartyDelegatedAuthorityConnector: ThirdPartyDelegatedAuthorityConnector
  ) extends UserInfoService {

  override def fetchUserInfo(version: Version)(implicit hc: HeaderCarrier): Future[UserInfo] = {
    def bearerToken(authorization: Authorization) = augmentString(authorization.value).stripPrefix("Bearer ")

    def scopes = hc.authorization match {
      case Some(authorization) => thirdPartyDelegatedAuthorityConnector.fetchScopes(bearerToken(authorization))
      case None => Future.failed(new UnauthorizedException("Bearer token is required"))
    }

    val userDetailsFetcher = version match {
      case Version_1_0 => v1AuthConnector.fetchUserDetails
      case Version_1_1 => v2AuthConnector.fetchUserDetails
    }

    scopes flatMap { scopes =>
      def getMaybeForScopes[T](maybeScopes: Set[String], allScopes: Set[String], f: => Future[Option[T]]): Future[Option[T]] = {
        if ((maybeScopes intersect allScopes).nonEmpty) f
        else Future.successful(None)
      }

      val scopesForAuthority = Set("openid:government-gateway", "email", "profile", "address", "openid:gov-uk-identifiers", "openid:hmrc-enrolments", "openid:mdtp")
      val maybeAuthority = getMaybeForScopes(scopesForAuthority, scopes, v1AuthConnector.fetchAuthority())

      val scopesForUserDetails = Set("openid:government-gateway", "email", "openid:mdtp")
      def maybeUserDetails = getMaybeForScopes[UserDetails](scopesForUserDetails, scopes, userDetailsFetcher)

      val scopesForDes = Set("profile", "address")
      def maybeDesUserInfo = {
        getMaybeForScopes[DesUserInfo](scopesForDes, scopes,
          maybeAuthority flatMap {
            case Some(auth) if auth.nino.isDefined => v1AuthConnector.fetchDesUserInfo
            case _ => Future.failed(new BadRequestException("NINO not found for this user"))
          }
        )
      }

      def maybeEnrolments = getMaybeForScopes[Enrolments](Set("openid:hmrc-enrolments"), scopes, v1AuthConnector.fetchEnrolments)

      for {
        authority <- maybeAuthority
        enrolments <- maybeEnrolments
        desUserInfo <- maybeDesUserInfo
        userDetails <- maybeUserDetails
      } yield
        userInfoTransformer.transform(scopes, authority, desUserInfo, enrolments, userDetails)
    }
  }
}

@Singleton
class SandboxUserInfoService @Inject() (userInfoGenerator : UserInfoGenerator) extends UserInfoService {
  override def fetchUserInfo(version: Version)(implicit hc: HeaderCarrier): Future[UserInfo] = {
    val generator : Gen[UserInfo] = version match {
      case Version_1_0 => userInfoGenerator.userInfoV1_0
      case Version_1_1 => userInfoGenerator.userInfoV1_1
    }

    Future.successful(generator.sample.getOrElse(UserInfo()))
  }
}
