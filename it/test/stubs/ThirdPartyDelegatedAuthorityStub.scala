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
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.json.Json

trait ThirdPartyDelegatedAuthorityStub {
  def willReturnScopesForAuthTokens(accessToken: String, scopes: Set[String]): StubMapping = {
    stubFor(
      get(urlPathEqualTo(s"/delegated-authority"))
        .withHeader("access-token", equalTo(accessToken))
        .willReturn(aResponse().withBody(s"""
          |{"token":
          | {
          |   "scopes":${Json.toJson(scopes)}
          | }
          |}
        """.stripMargin))
    )
  }
}
