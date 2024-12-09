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

import play.mvc.Http.Status.*
import play.api.libs.json.*
import play.api.mvc.Result
import play.api.mvc.Results.Status

/** Please use the constructors provided in the companion object and not the main constructor as these responses are expected to contain specific
  * wording.
  */
case class ErrorResponse private (httpStatusCode: Int, errorCode: String, message: String) {
  def toResult: Result = Status(httpStatusCode)(Json.toJson(this))
}

object ErrorResponse {
  implicit val writes: Writes[ErrorResponse] = (e: ErrorResponse) => Json.obj("code" -> e.errorCode, "message" -> e.message)

  def badGateway(msg: String = "Bad gateway"): ErrorResponse = ErrorResponse(BAD_GATEWAY, "BAD_GATEWAY", msg)
  def unauthorized(msg: String = "Bearer token is missing or not authorized"): ErrorResponse = ErrorResponse(UNAUTHORIZED, "UNAUTHORIZED", msg)
  def badRequest(msg: String = "Bad request"): ErrorResponse = ErrorResponse(BAD_REQUEST, "BAD_REQUEST", msg)
  def badRequest(errors: Seq[(JsPath, Seq[JsonValidationError])]): ErrorResponse = badRequest(JsError.toJson(errors).as[String])
  val acceptHeaderInvalid: ErrorResponse = ErrorResponse(NOT_ACCEPTABLE, "ACCEPT_HEADER_INVALID", "The accept header is missing or invalid")
  val notFound: ErrorResponse = ErrorResponse(NOT_FOUND, "NOT_FOUND", "Resource was not found")
  val unauthorizedLowCL: ErrorResponse = ErrorResponse(UNAUTHORIZED, "LOW_CONFIDENCE_LEVEL", "Confidence Level on account does not allow access")
  val internalServerError: ErrorResponse = ErrorResponse(INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "Internal server error")
  val preferencesSettingsError: ErrorResponse = ErrorResponse(INTERNAL_SERVER_ERROR, "PREFERENCE_SETTINGS_ERROR", "Failed to set preferences")
}
