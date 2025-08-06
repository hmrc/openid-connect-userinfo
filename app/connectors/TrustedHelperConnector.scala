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

import config.AppContext
import domain.TrustedHelper
import play.api.http.Status
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TrustedHelperConnector @Inject() (appContext: AppContext, httpClient: HttpClientV2) {
  val serviceUrl: String = appContext.fandfUrl

  def getDelegation()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[TrustedHelper]] = {
    httpClient
      .get(url"$serviceUrl/delegation/get")
      .execute(using readRaw, ec)
      .map { response =>
        response.status match {
          case Status.OK =>
            val helper = response.json.as[TrustedHelper]
            val updatedHelper =
              if (helper.return_link_url.startsWith("http://") || helper.return_link_url.startsWith("https://")) helper
              else helper.copy(return_link_url = appContext.platformHost.stripSuffix("/") + helper.return_link_url)
            Some(updatedHelper)
          case Status.NOT_FOUND => None
          case _                => throw new Exception(s"Unexpected response from trusted helper service: ${response.status}")
        }
      }
  }
}
