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

import uk.gov.hmrc.openidconnect.userinfo.connectors.AuthConnector

import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.http.{ HeaderCarrier, Upstream4xxResponse }

trait AuthService {
  val authConnector: AuthConnector


//  TODO - this is due to be replaced by auth-client wrapper finction on the controller method - PE-3211
  def isAuthorised()(implicit hc: HeaderCarrier) = {
    authConnector.fetchAuthority().map {
      case Some(auth) => true
      case None => false
    } recover {
      case Upstream4xxResponse(_, 401, _, _) => false
    }
  }
}

object AuthService extends AuthService {
  override val authConnector = AuthConnector
}
