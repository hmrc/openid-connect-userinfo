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

case class ErrorResponse(val httpStatusCode: Int, val errorCode: String, val message: String)

object ErrorResponse {
  implicit val writes: Writes[ErrorResponse] = (e: ErrorResponse) => Json.obj("code" -> e.errorCode, "message" -> e.message)

  def badGateway(msg: String = "Bad gateway"): ErrorResponse = ErrorResponse(BAD_GATEWAY, "BAD_GATEWAY", msg)
  def unauthorized(msg: String = "Bearer token is missing or not authorized"): ErrorResponse = ErrorResponse(UNAUTHORIZED, "UNAUTHORIZED", msg)
  def badRequest(msg: String = "Bad request"): ErrorResponse = ErrorResponse(BAD_REQUEST, "BAD_REQUEST", msg)
  def notAcceptable(msg: String = "The accept header is invalid"): ErrorResponse = ErrorResponse(NOT_ACCEPTABLE, "ACCEPT_HEADER_INVALID", msg)
}
