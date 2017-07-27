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

object UserDetailsStub extends Stub {
  override val stub: MockHost = new MockHost(22224)

  def willReturnUserDetailsWith(email: String) = {
    stub.mock.register(get(urlPathEqualTo(s"/uri/to/userDetails"))
      .willReturn(aResponse().withBody(
        s"""
           |{
           |   "affinityGroup": "Individual",
           |   "credentialRole": "Admin",
           |   "email": "$email",
           |   "agentId": "ACC",
           |   "agentCode": "AC-12345",
           |   "agentFriendlyName": "AC Accounting"
           |}
        """.stripMargin
      )))
  }

}
