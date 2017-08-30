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
import play.api.libs.json.Json
import uk.gov.hmrc.auth.core.retrieve.ItmpAddress
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.openidconnect.userinfo.domain.DesUserInfo
import uk.gov.hmrc.play.auth.microservice.connectors.ConfidenceLevel

object AuthStub extends Stub {
  override val stub: MockHost = new MockHost(22221)
  val optionalElement = PartialFunction[Option[String], String](_.map(s => s""""$s"""").getOrElse("null"))

  def willReturnAuthorityWith(confidenceLevel: ConfidenceLevel, nino: Nino): Unit = {
    val body =
      s"""
         |{
         |   "credentialStrength:": "strong",
         |   "confidenceLevel": ${confidenceLevel.level},
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

  def willAuthorise(desUserInfo: DesUserInfo): Unit = {
    val name = desUserInfo.name
    val address = desUserInfo.address
    val jsonAddress = Json.parse(s"""{
                                     |		"line1": ${optionalElement(address.line1)},
                                     |		"line2": ${optionalElement(address.line2)},
                                     |		"line3": ${optionalElement(address.line3)},
                                     |		"line4": ${optionalElement(address.line4)},
                                     |		"line5": ${optionalElement(address.line5)},
                                     |		"postCode": ${optionalElement(address.postCode)},
                                     |		"countryName": ${optionalElement(address.countryName)},
                                     |		"countryCode": ${optionalElement(address.countryCode)}
                                     |	}""".stripMargin)
    val jsonName =
      Json.parse(s"""{
         |		"givenName": ${optionalElement(name.givenName)},
         |		"middleName": ${optionalElement(name.middleName)},
         |		"familyName": ${optionalElement(name.familyName)}
         |}""".stripMargin)

    stub.mock.register(post(urlPathEqualTo(s"/auth/authorise"))
      .willReturn(aResponse()
        .withBody(Json.obj("itmpName" -> jsonName, "itmpDateOfBirth" -> desUserInfo.dateOfBirth, "itmpAddress" -> jsonAddress).toString())
        .withStatus(200))
    )
  }

  def willNotFindUser(): Unit = {
    stub.mock.register(post(urlPathEqualTo(s"/auth/authorise"))
      .willReturn(aResponse()
        .withBody("{}")
        .withStatus(404))
    )
  }

  def willAuthoriseWithEmptyResponse: Unit = {
    stub.mock.register(post(urlPathEqualTo(s"/auth/authorise"))
      .willReturn(aResponse()
        .withBody("{}")
        .withStatus(200))
    )
  }

  def willNotAuthorise(statusCode: Int = 401): Unit = {
    stub.mock.register(post(urlPathEqualTo(s"/auth/authorise"))
      .willReturn(aResponse()
          .withHeader("WWW-Authenticate", """MDTP detail="InsufficientConfidenceLevel"""")
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

  private def displayAddress(address: ItmpAddress): Boolean = {
    val ia = address
    Seq(ia.countryCode, ia.countryName, ia.postCode, ia.line1, ia.line2, ia.line3, ia.line4,
      ia.line5, ia.postCode).nonEmpty
  }
}
