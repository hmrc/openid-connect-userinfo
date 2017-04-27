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

  def willReturnAuthorityWith(confidenceLevel: ConfidenceLevel, nino: Nino) = {
    stub.mock.register(get(urlPathEqualTo(s"/auth/authority"))
      .willReturn(aResponse().withBody(
        s"""
          |{
          |   "confidenceLevel": ${confidenceLevel.level},
          |   "nino": "${nino.nino}",
          |   "enrolments": "/auth/oid/2/enrolments"
          |}
        """.stripMargin
      )))
  }

  def willReturnEnrolmentsWith() = {
    stub.mock.register(get(urlPathEqualTo(s"/auth/oid/2/enrolments"))
      .willReturn(aResponse().withBody(
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
    )))
  }
}
