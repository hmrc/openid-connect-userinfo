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

package domain

import play.api.libs.json.*

case class APIAccess(`type`: String)

object APIAccess {
  implicit val writes: Writes[APIAccess] = new Writes[APIAccess] {
    // [GG-9032], see also Tech Blog dated 18 May 2026
    def writes(o: APIAccess): JsValue = o.`type`.toUpperCase match {
      case "PUBLIC"               => JsString("PUBLIC")
      case "INTERNAL" | "PRIVATE" => JsString("INTERNAL")
      case x                      => throw new IllegalArgumentException(s"Illegal API access type: $x")
    }
  }
}
