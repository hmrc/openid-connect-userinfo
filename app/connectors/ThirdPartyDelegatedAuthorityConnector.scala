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

package connectors

import javax.inject.{Inject, Singleton}
import play.api.http.Status

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.{HeaderCarrier, HttpGet, HttpResponse, NotFoundException}
import config.AppContext
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw

import scala.util.control.NonFatal

@Singleton
class ThirdPartyDelegatedAuthorityConnector @Inject() (appContext: AppContext, http: HttpGet) {
  val serviceUrl: String = appContext.thirdPartyDelegatedAuthorityUrl

  def fetchScopes(accessToken: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Set[String]] = {
    http.GET(s"$serviceUrl/delegated-authority", Nil, Seq("access-token" -> accessToken))(readRaw, hc, ec) map { response =>
      if (response.status == Status.NOT_FOUND) {
        Set[String]()
      } else {
        (response.json \ "token" \ "scopes").as[Set[String]]
      }
    }
  }
}
