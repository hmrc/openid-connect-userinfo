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

import play.api.libs.json.{Json, Writes}

package object controllers {

  private def errorWrites[T <: ErrorResponse]: Writes[T] = (o: T) => Json.obj("code" -> o.errorCode, "message" -> o.message)

  implicit val errorResponseWrites: Writes[ErrorResponse] = errorWrites[ErrorResponse]
  implicit val errorUnauthorizedWrites: Writes[ErrorUnauthorized] = errorWrites[ErrorUnauthorized]
  implicit val errorNotFoundWrites: Writes[ErrorNotFound] = errorWrites[ErrorNotFound]
  implicit val errorAcceptHeaderInvalidWrites: Writes[ErrorAcceptHeaderInvalid] = errorWrites[ErrorAcceptHeaderInvalid]
  implicit val errorBadGatewayWrites: Writes[ErrorBadGateway] = errorWrites[ErrorBadGateway]
  implicit val errorBadRequestWrites: Writes[ErrorBadRequest] = errorWrites[ErrorBadRequest]
}
