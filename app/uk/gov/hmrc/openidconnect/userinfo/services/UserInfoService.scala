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

import uk.gov.hmrc.http.logging.Authorization
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, UnauthorizedException}
import uk.gov.hmrc.openidconnect.userinfo.connectors._
import uk.gov.hmrc.openidconnect.userinfo.data.UserInfoGenerator
import uk.gov.hmrc.openidconnect.userinfo.domain._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait UserInfoService {
  def fetchUserInfo()(implicit hc: HeaderCarrier): Future[UserInfo]
}

trait LiveUserInfoService extends UserInfoService {
  val authConnector: AuthConnector
  val userInfoTransformer: UserInfoTransformer
  val thirdPartyDelegatedAuthorityConnector: ThirdPartyDelegatedAuthorityConnector

  override def fetchUserInfo()(implicit hc: HeaderCarrier): Future[UserInfo] = {
    def bearerToken(authorization: Authorization) = augmentString(authorization.value).stripPrefix("Bearer ")

    def scopes = hc.authorization match {
      case Some(authorization) => thirdPartyDelegatedAuthorityConnector.fetchScopes(bearerToken(authorization))
      case None => Future.failed(new UnauthorizedException("Bearer token is required"))
    }

    scopes flatMap { scopes =>
      def getMaybeForScopes[T](maybeScopes: Set[String], allScopes: Set[String], f: => Future[Option[T]]): Future[Option[T]] = {
        if ((maybeScopes intersect allScopes).nonEmpty) f
        else Future.successful(None)
      }

      def getMaybeByParamForScopes[I, O](maybeScopes: Set[String], allScopes: Set[String], param: I, f: I => Future[Option[O]]): Future[Option[O]] = {
        if ((maybeScopes intersect allScopes).nonEmpty) f(param)
        else Future.successful(None)
      }

      val scopesForAuthority = Set("openid:government-gateway", "email", "profile", "address", "openid:gov-uk-identifiers", "openid:hmrc-enrolments", "openid:mdtp")
      val maybeAuthority = getMaybeForScopes(scopesForAuthority, scopes, authConnector.fetchAuthority)

      val scopesForDes = Set("profile", "address")
      val maybeDesUserInfo = maybeAuthority flatMap { authority =>
        val concreteAuthority = authority.getOrElse(Authority())
        getMaybeByParamForScopes[Authority, DesUserInfo](scopesForDes, scopes, concreteAuthority,
          concreteAuthority.nino match {
            case Some(_) => authConnector.fetchDesUserInfo
            case _ => (_) => Future.failed(new BadRequestException("NINO not found for this user"))
          }
        )
      }

      val scopesForUserDetails = Set("openid:government-gateway", "email", "openid:mdtp")
      val maybeUserDetails = maybeAuthority flatMap { authority =>
        getMaybeByParamForScopes[Authority, UserDetails](scopesForUserDetails, scopes, authority.getOrElse(Authority()), _ => authConnector.fetchUserDetails)
      }

      def maybeEnrolments = maybeAuthority flatMap { authority =>
        getMaybeByParamForScopes[Authority, Seq[Enrolment]](Set("openid:hmrc-enrolments"), scopes,
          authority.getOrElse(Authority()), authConnector.fetchEnrolments)
      }

      for {
        authority <- maybeAuthority
        enrolments <- maybeEnrolments
        desUserInfo <- maybeDesUserInfo
        userDetails <- maybeUserDetails
      } yield
        userInfoTransformer.transform(scopes, desUserInfo, enrolments, authority, userDetails)
    }
  }
}

trait SandboxUserInfoService extends UserInfoService {
  val userInfoGenerator: UserInfoGenerator

  override def fetchUserInfo()(implicit hc: HeaderCarrier): Future[UserInfo] = {
    Future.successful(userInfoGenerator.userInfo.sample.getOrElse(UserInfo()))
  }
}

object LiveUserInfoService extends LiveUserInfoService {
  override val authConnector = AuthConnector
  override val userInfoTransformer = UserInfoTransformer
  override val thirdPartyDelegatedAuthorityConnector = ThirdPartyDelegatedAuthorityConnector
}

object SandboxUserInfoService extends SandboxUserInfoService {
  override val userInfoGenerator = UserInfoGenerator
}
