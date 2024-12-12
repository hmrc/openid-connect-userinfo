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

package controllers

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.Materializer
import org.scalatest.matchers.should.Matchers
import play.api.http.Status.*
import testSupport.UnitSpec

class ErrorResponseSpec extends UnitSpec with Matchers {
  implicit lazy val system: ActorSystem = ActorSystem()
  implicit lazy val materializer: Materializer = Materializer(system)

  "error response should serialise with the expected code and description" when {
    "unauthorized" in {
      val result = ErrorResponse.unauthorized().toResult
      status(result) shouldBe UNAUTHORIZED
      bodyOf(result) shouldBe """{"code":"UNAUTHORIZED","message":"Bearer token is missing or not authorized"}"""
    }

    "notFound" in {
      val result = ErrorResponse.notFound.toResult
      status(result) shouldBe NOT_FOUND
      bodyOf(result) shouldBe """{"code":"NOT_FOUND","message":"Resource was not found"}"""
    }

    "badGateway" in {
      val result = ErrorResponse.badGateway().toResult
      status(result) shouldBe BAD_GATEWAY
      bodyOf(result) shouldBe """{"code":"BAD_GATEWAY","message":"Bad gateway"}"""
    }

    "badRequest" in {
      val result = ErrorResponse.badRequest().toResult
      status(result) shouldBe BAD_REQUEST
      bodyOf(result) shouldBe """{"code":"BAD_REQUEST","message":"Bad request"}"""
    }

    "unauthorizedLowCL" in {
      val result = ErrorResponse.unauthorizedLowCL.toResult
      status(result) shouldBe UNAUTHORIZED
      bodyOf(result) shouldBe """{"code":"LOW_CONFIDENCE_LEVEL","message":"Confidence Level on account does not allow access"}"""
    }

    "acceptHeaderInvalid" in {
      val result = ErrorResponse.acceptHeaderInvalid.toResult
      status(result) shouldBe NOT_ACCEPTABLE
      bodyOf(result) shouldBe """{"code":"ACCEPT_HEADER_INVALID","message":"The accept header is missing or invalid"}"""
    }

    "internalServerError" in {
      val result = ErrorResponse.internalServerError.toResult
      status(result) shouldBe INTERNAL_SERVER_ERROR
      bodyOf(result) shouldBe """{"code":"INTERNAL_SERVER_ERROR","message":"Internal server error"}"""
    }

    "preferencesSettingsError" in {
      val result = ErrorResponse.preferencesSettingsError.toResult
      status(result) shouldBe INTERNAL_SERVER_ERROR
      bodyOf(result) shouldBe """{"code":"PREFERENCE_SETTINGS_ERROR","message":"Failed to set preferences"}"""
    }
  }
}
