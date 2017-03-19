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

import play.api.Logger
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.openidconnect.userinfo.connectors.{AuthConnector, DesConnector, ThirdPartyDelegatedAuthorityConnector}
import uk.gov.hmrc.openidconnect.userinfo.data.UserInfoGenerator
import uk.gov.hmrc.openidconnect.userinfo.domain._
import uk.gov.hmrc.play.http.logging.Authorization
import uk.gov.hmrc.play.http.{HeaderCarrier, UnauthorizedException}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success}

trait UserInfoService {
  def fetchUserInfo()(implicit hc: HeaderCarrier): Future[Option[UserInfo]]
}

trait LiveUserInfoService extends UserInfoService {
  val authConnector: AuthConnector
  val desConnector: DesConnector
  val userInfoTransformer: UserInfoTransformer
  val thirdPartyDelegatedAuthorityConnector: ThirdPartyDelegatedAuthorityConnector


  override def fetchUserInfo()(implicit hc: HeaderCarrier): Future[Option[UserInfo]] = {
    def bearerToken(authorization: Authorization) = augmentString(authorization.value).stripPrefix("Bearer ")

    def scopes = hc.authorization match {
      case Some(authorization) => thirdPartyDelegatedAuthorityConnector.fetchScopes(bearerToken(authorization))
      case None => Future.failed(new UnauthorizedException("Bearer token is required"))
    }

    val promiseNino = Promise[Option[Nino]]()
    val maybeNino = promiseNino.future

    scopes flatMap { scopes =>
      val scopesForNino = Set("profile", "address", "openid:gov-uk-identifiers")
      if ((scopesForNino -- scopes).size != scopesForNino.size) {
        authConnector.fetchNino() onComplete {
          case Success(nino) => promiseNino success nino
          case Failure(exception) => throw exception
        }
      } else promiseNino success None

      val promiseDesUserInfo = Promise[Option[DesUserInfo]]
      val maybeDesUserInfo = promiseDesUserInfo.future

      val scopesDes = Set("profile", "address")
      if ((scopesDes -- scopes).size != scopesDes.size) {
        maybeNino onComplete {
          case Success(hopefulyNino) => desConnector.fetchUserInfo(hopefulyNino) onComplete {
            case Success(desUserInfo) => promiseDesUserInfo success desUserInfo
            case Failure(exception) => throw exception
          }
          case Failure(exception) => throw exception
        }
      } else promiseDesUserInfo success None

      def maybeEnrolments = if (scopes.contains("openid:hmrc_enrolments")) authConnector.fetchEnrolments() else Future.successful(None)

      val future: Future[UserInfo] = for {
        nino <- maybeNino
        enrolments <- maybeEnrolments
        desUserInfo <- maybeDesUserInfo
      } yield userInfoTransformer.transform(scopes, desUserInfo, nino, enrolments)

      future map (Some((_: UserInfo))) recover {
        case e: Throwable => {
          Logger.debug(e.getMessage, e)
          None
        }
      }
    }
  }
}

trait SandboxUserInfoService extends UserInfoService {
  val userInfoGenerator: UserInfoGenerator

  override def fetchUserInfo()(implicit hc: HeaderCarrier): Future[Option[UserInfo]] = {
    Future.successful(userInfoGenerator.userInfo.sample)
  }
}

object LiveUserInfoService extends LiveUserInfoService {
  override val desConnector = DesConnector
  override val authConnector = AuthConnector
  override val userInfoTransformer = UserInfoTransformer
  override val thirdPartyDelegatedAuthorityConnector = ThirdPartyDelegatedAuthorityConnector
}

object SandboxUserInfoService extends SandboxUserInfoService {
  override val userInfoGenerator = UserInfoGenerator
}
