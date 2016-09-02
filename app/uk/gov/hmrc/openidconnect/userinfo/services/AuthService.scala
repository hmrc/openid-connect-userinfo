/*
 * Copyright 2016 HM Revenue & Customs
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
import uk.gov.hmrc.play.auth.microservice.connectors.ConfidenceLevel.L200
import uk.gov.hmrc.play.http.HeaderCarrier
import scala.concurrent.ExecutionContext.Implicits.global

trait AuthService {
  val authConnector: AuthConnector

  def isAuthorised()(implicit hc: HeaderCarrier) = {
    authConnector.confidenceLevel().map {
      case Some(cf) => cf >= L200.level
      case None => false
    }
  }
}

object AuthService extends AuthService {
  override val authConnector = AuthConnector
}
