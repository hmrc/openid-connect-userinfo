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
import play.api.libs.json._
import uk.gov.hmrc.auth.core.authorise.{AffinityGroup, CredentialRole}
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.openidconnect.userinfo.domain.DesUserInfo
import uk.gov.hmrc.play.auth.microservice.connectors.ConfidenceLevel
import uk.gov.hmrc.play.controllers.RestFormats.localDateFormats
import uk.gov.hmrc.openidconnect.userinfo.domain._

object AuthStub extends Stub {
  override val stub: MockHost = new MockHost(22221)
  val optionalElement = PartialFunction[Option[String], String](_.map(s => s""""$s"""").getOrElse("null"))

  implicit class JsOptAppendable(jsObject: JsObject) {
    def appendOptional(key: String, value: Option[JsValue]): JsObject = value.map(js => jsObject + (key -> js))
      .getOrElse(jsObject)
  }

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

  def willAuthorise(desUserInfo: Option[DesUserInfo] = None, agentInformation: Option[AgentInformation] = None,
                    credentials: Option[Credentials] = None, name: Option[Name] = None, email: Option[Email] = None,
                    affinityGroup: Option[AffinityGroup] = None, role: Option[CredentialRole] = None,
                    mdtp: Option[MdtpInformation] = None, gatewayInformation: Option[GatewayInformation] = None): Unit = {
    implicit val agentWrites = Json.writes[AgentInformation]
    implicit val credentialWrites = Json.writes[Credentials]
    implicit val nameWrites = Json.writes[Name]
    val jsonAddress: Option[JsValue] = desUserInfo.map(d => Json.toJson(d.address))
    val jsonItmpName: Option[JsValue] = desUserInfo.map(d => Json.toJson(d.name))
    val jsonAgent: Option[JsValue] = agentInformation.map(Json.toJson(_))
    val jsonCredentials: Option[JsValue] = credentials.map(Json.toJson(_))
    val jsonName : Option[JsValue] = name.map(Json.toJson(_))
    val jsonDob = desUserInfo.flatMap(_.dateOfBirth)
    val jsonMdtp: Option[JsValue] = mdtp.map(Json.toJson(_))
    val jsonGatewayInformation: Option[JsValue] = gatewayInformation.map(Json.toJson(_))

    val response = Json.obj()
      .appendOptional("itmpName", jsonItmpName)
      .appendOptional("itmpDateOfBirth", jsonDob.map(localDateFormats.writes))
      .appendOptional("itmpAddress", jsonAddress)
      .appendOptional("agentInformation", jsonAgent)
      .appendOptional("name", jsonName)
      .appendOptional("credentials", jsonCredentials)
      .appendOptional("email", email.map(e => JsString(e.value)))
      .appendOptional("affinityGroup", affinityGroup.map(ag => AffinityGroup.jsonFormat.writes(ag)))
      .appendOptional("credentialRole", role.map(r => CredentialRole.jsonFormat.writes(r)))
      .appendOptional("agentCode", agentInformation.flatMap(a => a.agentCode.map(JsString)))
      .appendOptional("mdtpInformation", jsonMdtp)
      .appendOptional("gatewayInformation", jsonGatewayInformation)

    stub.mock.register(post(urlPathEqualTo(s"/auth/authorise"))
      .willReturn(aResponse()
        .withBody(response.toString())
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
