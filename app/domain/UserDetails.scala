/*
 * Copyright 2022 HM Revenue & Customs
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

import org.joda.time.LocalDate
import uk.gov.hmrc.auth.core.retrieve.{GatewayInformation, MdtpInformation}

case class UserDetails(
    authProviderId:     Option[String]             = None,
    authProviderType:   Option[String]             = None,
    name:               Option[String]             = None,
    lastName:           Option[String]             = None,
    dateOfBirth:        Option[LocalDate]          = None,
    postCode:           Option[String]             = None,
    email:              Option[String]             = None,
    affinityGroup:      Option[String]             = None,
    agentCode:          Option[String]             = None,
    agentFriendlyName:  Option[String]             = None,
    credentialRole:     Option[String]             = None,
    description:        Option[String]             = None,
    groupIdentifier:    Option[String]             = None,
    agentId:            Option[String]             = None,
    gatewayInformation: Option[GatewayInformation],
    mdtpInformation:    Option[MdtpInformation],
    unreadMessageCount: Option[Int],
    profile:            Option[String],
    groupProfile:       Option[String]
)
