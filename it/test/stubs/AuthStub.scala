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

package stubs

import com.github.tomakehurst.wiremock.client.WireMock.*
import controllers.{Version, Version_1_0}
import domain.*
import play.api.libs.json.*
import play.api.libs.json.Writes.DefaultLocalDateWrites
import uk.gov.hmrc.auth.core.retrieve.*
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.{AffinityGroup, CredentialRole}
import uk.gov.hmrc.domain.Nino

trait AuthStub {

  private implicit val itmpNameFormat: OFormat[ItmpName] = Json.format
  private implicit val itmpAddressFormat: OFormat[ItmpAddress] = Json.format

  private implicit val gatewayInformationFormat: Format[GatewayInformation] = Json.format
  private implicit val mdtpInformationFormat: Format[MdtpInformation] = Json.format

  implicit class JsOptAppendable(jsObject: JsObject) {
    def appendOptional(key: String, value: Option[JsValue]): JsObject = value
      .map(js => jsObject + (key -> js))
      .getOrElse(jsObject)
  }

  def willAuthoriseWith(statusCode: Int, body: String = Json.obj().toString()): Unit = {
    stubFor(
      post(urlPathEqualTo(s"/auth/authorise"))
        .withRequestBody(
          equalToJson(
            Json
              .obj(
                "authorise" -> JsArray(),
                "retrieve"  -> JsArray()
              )
              .toString()
          )
        )
        .willReturn(aResponse().withBody(body).withStatus(statusCode))
    )
  }

  def willReturnAuthorityWith(nino: Nino): Unit = {
    val response = Json.obj(
      "nino"                -> nino,
      "optionalCredentials" -> Json.obj("providerId" -> "1304372065861347", "providerType" -> "GG")
    )
    stubFor(
      post(urlPathEqualTo(s"/auth/authorise"))
        .withRequestBody(
          equalToJson(
            Json
              .obj(
                "authorise" -> JsArray(),
                "retrieve"  -> JsArray((Retrievals.credentials and Retrievals.nino).propertyNames.map(JsString(_)))
              )
              .toString()
          )
        )
        .willReturn(
          aResponse()
            .withBody(response.toString())
            .withStatus(200)
        )
    )
  }

  def willFindUserFailed(status: Int): Unit = {
    stubFor(
      post(urlPathEqualTo(s"/auth/authorise"))
        .withRequestBody(
          equalToJson(
            Json
              .obj(
                "authorise" -> JsArray(),
                "retrieve"  -> JsArray(Retrievals.allItmpUserDetails.propertyNames.map(JsString(_)))
              )
              .toString()
          )
        )
        .willReturn(
          aResponse()
            .withBody("{}")
            .withStatus(status)
        )
    )
  }

  def willFindUser(desUserInfo: Option[DesUserInfo] = None,
                   agentInformation: Option[AgentInformation] = None,
                   credentials: Option[Credentials] = None,
                   name: Option[Name] = None,
                   email: Option[Email] = None,
                   affinityGroup: Option[AffinityGroup] = None,
                   role: Option[CredentialRole] = None,
                   mdtp: Option[MdtpInformation] = None,
                   gatewayInformation: Option[GatewayInformation] = None,
                   unreadMessageCount: Option[Int] = None,
                   profileUrl: Option[String] = None,
                   groupProfileUrl: Option[String] = None,
                   version: Version = Version_1_0
                  ): Unit = {
    implicit val agentWrites: OWrites[AgentInformation] = Json.writes[AgentInformation]
    implicit val credentialWrites: OWrites[Credentials] = Json.writes[Credentials]
    implicit val nameWrites: OWrites[Name] = Json.writes[Name]
    val jsonAddress: Option[JsValue] = desUserInfo.flatMap(_.address).map(a => Json.toJson(a))
    val jsonItmpName: Option[JsValue] = desUserInfo.flatMap(_.name).map(n => Json.toJson(n))
    val jsonAgent: Option[JsValue] = agentInformation.map(Json.toJson(_))
    val jsonCredentials: Option[JsValue] = credentials.map(Json.toJson(_))
    val jsonName: Option[JsValue] = name.map(Json.toJson(_))
    val jsonDob = desUserInfo.flatMap(_.dateOfBirth)
    val jsonMdtp: Option[JsValue] = mdtp.map(Json.toJson(_))
    val jsonGatewayInformation: Option[JsValue] = gatewayInformation.map(Json.toJson(_))

    val response = Json
      .obj()
      .appendOptional("optionalItmpName", jsonItmpName)
      .appendOptional("itmpDateOfBirth", jsonDob.map(DefaultLocalDateWrites.writes))
      .appendOptional("optionalItmpAddress", jsonAddress)
      .appendOptional("agentInformation", jsonAgent)
      .appendOptional("email", email.map(e => JsString(e.value)))
      .appendOptional("affinityGroup", affinityGroup.map(ag => AffinityGroup.jsonFormat.writes(ag)))
      .appendOptional("credentialRole", role.map(r => CredentialRole.reads.writes(r)))
      .appendOptional("agentCode", agentInformation.flatMap(a => a.agentCode.map(JsString(_))))
      .appendOptional("mdtpInformation", jsonMdtp)
      .appendOptional("gatewayInformation", jsonGatewayInformation)
      .appendOptional("unreadMessageCount", unreadMessageCount.map(Json.toJson(_)))

    val v10response = response
      .appendOptional("optionalName", jsonName)
      .appendOptional("optionalCredentials", jsonCredentials)

    version match {
      case Version_1_0 =>
        stubFor(
          post(urlPathEqualTo(s"/auth/authorise"))
            .withRequestBody(
              equalToJson(
                Json
                  .obj(
                    "authorise" -> JsArray(),
                    "retrieve" -> JsArray(
                      (Retrievals.allUserDetails and Retrievals.mdtpInformation and Retrievals.gatewayInformation).propertyNames.map(JsString(_))
                    )
                  )
                  .toString()
              )
            )
            .willReturn(
              aResponse()
                .withBody(v10response.toString())
                .withStatus(200)
            )
        )
    }

    stubFor(
      post(urlPathEqualTo(s"/auth/authorise"))
        .withRequestBody(
          equalToJson(
            Json
              .obj(
                "authorise" -> JsArray(),
                "retrieve"  -> JsArray(Retrievals.allItmpUserDetails.propertyNames.map(JsString(_)))
              )
              .toString()
          )
        )
        .willReturn(
          aResponse()
            .withBody(v10response.toString())
            .withStatus(200)
        )
    )
  }

  def willNotFindUser(): Unit = {
    stubFor(
      post(urlPathEqualTo(s"/auth/authorise"))
        .withRequestBody(
          equalToJson(
            Json
              .obj(
                "authorise" -> JsArray(),
                "retrieve"  -> JsArray(Retrievals.allItmpUserDetails.propertyNames.map(JsString(_)))
              )
              .toString()
          )
        )
        .willReturn(
          aResponse()
            .withBody("{}")
            .withStatus(404)
        )
    )

    stubFor(
      post(urlPathEqualTo(s"/auth/authorise"))
        .withRequestBody(
          equalToJson(
            Json
              .obj(
                "authorise" -> JsArray(),
                "retrieve" -> JsArray(
                  (Retrievals.allUserDetails and Retrievals.mdtpInformation and Retrievals.gatewayInformation).propertyNames.map(JsString(_))
                )
              )
              .toString()
          )
        )
        .willReturn(
          aResponse()
            .withBody("{}")
            .withStatus(404)
        )
    )
  }

  def willReturnEnrolmentsWith(): Unit = {
    val body =
      s"""
         |{"allEnrolments": [
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
         |]}
     """.stripMargin
    willReturnEnrolmentsWith(200, body)
  }

  def willReturnEnrolmentsWith(statusCode: Int, body: String = ""): Unit = {
    stubFor(
      post(urlPathEqualTo(s"/auth/authorise"))
        .withRequestBody(
          equalToJson(
            Json
              .obj(
                "authorise" -> JsArray(),
                "retrieve"  -> JsArray(Retrievals.allEnrolments.propertyNames.map(JsString(_)))
              )
              .toString()
          )
        )
        .willReturn(
          aResponse()
            .withBody(body)
            .withStatus(statusCode)
        )
    )
  }

}
