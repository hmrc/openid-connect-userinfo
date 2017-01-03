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

package unit.uk.gov.hmrc.openidconnect.userinfo

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.client.{MappingBuilder, UrlMatchingStrategy, WireMock}

trait WiremockDSL {

  def given() = new Givens()

  class Givens() {
    def get(strategy: UrlMatchingStrategy) = new Result(WireMock.get(strategy))

    class Result(mappingBuilder: MappingBuilder) {
      def returns(responseBody: String) = {
        stubFor(mappingBuilder.willReturn(aResponse()
          .withStatus(200)
          .withHeader("Content-Type", "application/json")
          .withBody(responseBody)))
      }

      def returns(statusCode: Int) = {
        stubFor(mappingBuilder.willReturn(aResponse()
          .withStatus(statusCode)))
      }
    }
  }
}
