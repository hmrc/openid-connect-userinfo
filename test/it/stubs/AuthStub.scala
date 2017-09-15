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

package it.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import it.{MockHost, Stub}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.auth.microservice.connectors.ConfidenceLevel

object AuthStub extends Stub {
  override val stub: MockHost = new MockHost(22221)

  def willReturnAuthorityWith(nino: Nino): Unit = {
    val body =
      s"""
         |{
         |   "credentialStrength:": "strong",
         |   "confidenceLevel": 0,
         |   "userDetailsLink": "http://localhost:22224/uri/to/userDetails",
         |   "nino": "${nino.nino}",
         |   "enrolments": "/auth/oid/2/enrolments",
         |   "affinityGroup": "Individual",
         |   "credId": "1304372065861347",
         |   "gatewayToken": "gateway-token-qwert"
         |}
        """.stripMargin
    willReturnAuthorityWith(200, body)
  }

  def willReturnAuthorityWith(statusCode: Int, body: String = ""): Unit = {
    stub.mock.register(get(urlPathEqualTo(s"/auth/authority"))
      .willReturn(aResponse()
        .withBody(body)
        .withStatus(statusCode))
    )
  }

  def willReturnEnrolmentsWith(): Unit = {
    val body =
      s"""
         |[
         |  {
         |    "key": "IR-SA",
         |    "identifiers": [
         |       {
         |         "key": "UTR",
         |         "value": "174371121"
         |       }
         |    ],
         |    "state": "Activated"
         |  }
         |]
     """.stripMargin
    willReturnEnrolmentsWith(200, body)
  }

  def willReturnEnrolmentsWith(statusCode: Int, body: String = ""): Unit = {
    stub.mock.register(get(urlPathEqualTo(s"/auth/oid/2/enrolments"))
      .willReturn(aResponse()
        .withBody(body)
        .withStatus(statusCode)))
  }
}
