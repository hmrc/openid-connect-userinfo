/*
 * Copyright 2016 HM Revenue & Customs
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

case object ErrorSaUtrInvalid extends ErrorResponse(BAD_REQUEST, "SA_UTR_INVALID", "The provided SA UTR is invalid")

case object ErrorUnauthorized extends ErrorResponse(UNAUTHORIZED, "UNAUTHORIZED", "Bearer token is missing or not authorized")

case object ErrorNotFound extends ErrorResponse(NOT_FOUND, "NOT_FOUND", "Resource was not found")

case object ErrorGenericBadRequest extends ErrorResponse(BAD_REQUEST, "BAD_REQUEST", "Bad Request")

case object ErrorNotImplemented extends ErrorResponse(NOT_IMPLEMENTED, "NOT_IMPLEMENTED", "Live service endpoint is not implemented")

case object ErrorAcceptHeaderInvalid extends ErrorResponse(NOT_ACCEPTABLE, "ACCEPT_HEADER_INVALID", "The accept header is missing or invalid")

case object ErrorInternalServerError extends ErrorResponse(INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "Internal server error")
