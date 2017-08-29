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

package unit.uk.gov.hmrc.openidconnect.userinfo.connectors

import org.joda.time.LocalDate
import org.scalatest.BeforeAndAfterEach
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.{ItmpAddress, ItmpName, ~, _}
import uk.gov.hmrc.openidconnect.userinfo.config.WSHttp
import uk.gov.hmrc.openidconnect.userinfo.connectors.DesConnector
import uk.gov.hmrc.openidconnect.userinfo.domain._
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet, Upstream5xxResponse}
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DesConnectorSpec extends UnitSpec with BeforeAndAfterEach with WithFakeApplication {

  type ItmpDataType = ~[~[ItmpName, Option[LocalDate]], ItmpAddress]
  var itmpDataFuture: Future[ItmpDataType] = _

  trait Setup {
    implicit val hc = HeaderCarrier()

    val connector = new DesConnector {
      override val http: HttpGet = WSHttp

      def authConnector = new AuthConnector {
        override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit hc: HeaderCarrier): Future[A] = {
          itmpDataFuture.map(_.asInstanceOf[A])
        }
      }
    }
  }

  override def beforeEach() {
    itmpDataFuture = Future.failed(new IllegalMonitorStateException("Initialisation required"))
  }

  override def afterEach() {
  }

  "fetch user info" should {
    val nino = Some("AA111111A")
    val ninoWithoutSuffix = "AA111111"
    val authority: Authority = Authority(Some("weak"), Some(200), nino, Some("/uri/to/userDetails"),
      Some("/uri/to/enrolments"), Some("Individual"), Some("1304372065861347"))

    "return the user info" in new Setup {
      val profile = ItmpName(Some("Andrew"), Some("John"), Some("Smith"))
      val address = ItmpAddress(Some("1 Station Road"), Some("Town Centre"), Some("Sometown"),
        Some("Anyshire"), Some("UK"), Some("AB12 3CD"), Some("UK"), Some("1"))
      itmpDataFuture = Future.successful(new ~(new ~(profile, Option(LocalDate.parse("1980-01-01"))), address))

      val result = await(connector.fetchUserInfo(authority))

      result shouldBe Some(DesUserInfo(
        ItmpName(Some("Andrew"), Some("John"), Some("Smith")),
        Some(LocalDate.parse("1980-01-01")),
        ItmpAddress(Some("1 Station Road"), Some("Town Centre"), Some("Sometown"), Some("Anyshire"), Some("UK"), Some("AB12 3CD"), Some("Great Britain"), Some("GB"))))
    }

    "return None when DES does not have an entry for the NINO" in new Setup {
      itmpDataFuture = Future.successful(null)
      val result = await(connector.fetchUserInfo(authority))

      result shouldBe None
    }

    "fail when DES returns a 500 response" in new Setup {
      itmpDataFuture = Future.failed(Upstream5xxResponse("Failure", 500, 500))

      intercept[Upstream5xxResponse] {
        await(connector.fetchUserInfo(authority))
      }
    }
  }
}
