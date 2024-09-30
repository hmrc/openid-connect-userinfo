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

case class GovernmentGatewayDetails(
  user_id: Option[String],
  roles: Option[Seq[String]],
  user_name: Option[String],
  affinity_group: Option[String],
  agent_code: Option[String],
  agent_id: Option[String],
  agent_friendly_name: Option[String],
  gateway_token: Option[String],
  unread_message_count: Option[Int] = None,
  profile_uri: Option[String],
  group_profile_uri: Option[String]
)
