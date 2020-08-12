/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.openidconnect.userinfo.controllers

import play.mvc.Http.Status._

sealed abstract class ErrorResponse(val httpStatusCode: Int,
                                    val errorCode: String,
                                    val message: String)

case class ErrorUnauthorized(msg: String = "Bearer token is missing or not authorized") extends ErrorResponse(UNAUTHORIZED, "UNAUTHORIZED", msg)

case class ErrorNotFound(msg: String = "Resource was not found") extends ErrorResponse(NOT_FOUND, "NOT_FOUND", msg)

case class ErrorAcceptHeaderInvalid(msg: String = "The accept header is missing or invalid") extends ErrorResponse(NOT_ACCEPTABLE, "ACCEPT_HEADER_INVALID", msg)

case class ErrorBadGateway(msg: String = "Bad gateway") extends ErrorResponse(BAD_GATEWAY, "BAD_GATEWAY", msg)

case class ErrorBadRequest(msg: String = "Bad request") extends ErrorResponse(BAD_REQUEST, "BAD_REQUEST", msg)
