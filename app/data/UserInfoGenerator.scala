/*
 * Copyright 2023 HM Revenue & Customs
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

package data

import javax.inject.Singleton
import java.time.LocalDate
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier}
import config.UserInfoFeatureSwitches
import domain.{Address, GovernmentGatewayDetails, Mdtp, UserInfo}

import scala.util.Random.{nextInt => randomNextInt}

@Singleton
class UserInfoGenerator {
  val firstNames: Seq[Option[String]] = Seq(Some("Roland"), Some("Eddie"), Some("Susanna"), Some("Jake"), Some("Oy"), Some("Cuthbert"), Some("Alain"), Some("Jamie"), Some("Thomas"), Some("Susan"), Some("Randall"), None)
  val middleNames: Seq[Option[String]] = Seq(Some("De"), Some("Donald"), Some("Billy"), Some("E"), Some("Alex"), Some("Abel"), None, None, None, None, None, None)
  val lastNames: Seq[Option[String]] = Seq(Some("Deschain"), Some("Dean"), Some("Dean"), Some("Chambers"), Some("Bumbler"), Some("Allgood"), Some("Johns"), Some("Curry"), Some("Whitman"), Some("Delgado"), Some("Flagg"), Some("Bowen"), None)
  val deviceId = "device-id-abc"
  val sessionId = "session-id-abcd"

  val fullAddress: Option[Address] = Some(Address(
    """221B Baker Street
      |Town centre
      |London
      |England
      |Line5
      |NW1 9NT
      |Great Britain""".stripMargin, Some("NW1 9NT"), Some("Great Britain"), Some("GB")))

  def addressWithToggleableFeatures(isAddressLine5: Boolean = false, isCountryCode: Boolean = false): Option[Address] = {
    val addressLine5 = if (isAddressLine5) "\n|Line5" else ""
    val code = if (isCountryCode) Some("GB") else None

    Some(Address(
      s"""221B Baker Street
        |Town centre
        |London
        |England$addressLine5
        |NW1 9NT
        |Great Britain""".stripMargin, Some("NW1 9NT"), Some("Great Britain"), code))
  }

  def address: Option[Address] = addressWithToggleableFeatures(UserInfoFeatureSwitches.addressLine5.isEnabled, UserInfoFeatureSwitches.countryCode.isEnabled)

  val enrolments: Set[Enrolment] = Set(Enrolment("IR-SA", List(EnrolmentIdentifier("UTR", "174371121")), "Activated"))
  private val government_gateway_v1_0: GovernmentGatewayDetails = GovernmentGatewayDetails(Some("32131"), Some(scala.collection.immutable.Seq("User")), Some("Chambers"), Some("affinityGroup"),
                                                                                           Some("agent-code-12345"), Some("agent-id-12345"), Some("agent-friendly-name-12345"), Some("gateway-token-val"), Some(10), None, None)
  private val government_gateway_v1_1: GovernmentGatewayDetails = GovernmentGatewayDetails(Some("32131"), Some(scala.collection.immutable.Seq("User")), Some("Chambers"), Some("affinityGroup"),
                                                                                           Some("agent-code-12345"), Some("agent-id-12345"), Some("agent-friendly-name-12345"), Some("gateway-token-val"), Some(10), Some("some_url"), Some("some_other_url"))
  val mdtp: Mdtp = Mdtp(deviceId, sessionId)

  private lazy val ninoPrefixes = "ABCEGHJKLMNPRSTWXYZ"

  private lazy val ninoSuffixes = "ABCD"

  private def firstNameGenerator: Option[String] = firstNames(randomNextInt(firstNames.size))
  private def lastNameGenerator: Option[String] = lastNames(randomNextInt(lastNames.size))
  private def middleNameGenerator: Option[String] = middleNames(randomNextInt(middleNames.size))
  private def dayGenerator: Int = 1 + randomNextInt(28)
  private def monthGenerator: Int = 1 + randomNextInt(12)
  private def yearGenerator: Int = 1940 + randomNextInt(50)
  private def ninoPrefixGenerator: Option[String] = Some(ninoPrefixes.charAt(randomNextInt(ninoPrefixes.length)).toString)
  private def ninoSuffixGenerator: Option[String] = Some(ninoSuffixes.charAt(randomNextInt(ninoSuffixes.length)).toString)
  private def numbersGenerator: Option[Int] = Some(100000 + randomNextInt(99999))

  private def email(name: Option[String], lastName: Option[String]): Option[String] = (name, lastName) match {
    case (Some(n), None)    => Some(s"$n@abc.com")
    case (None, Some(l))    => Some(s"$l@abc.com")
    case (Some(n), Some(l)) => Some(s"$n.$l@abc.com")
    case (None, None)       => None
  }

  private def dateOfBirth =
    LocalDate.of(yearGenerator, monthGenerator, dayGenerator)

  private def formattedNino = {
    val first = ninoPrefixGenerator.getOrElse("")
    val second = ninoPrefixGenerator.getOrElse("")
    val number = numbersGenerator.getOrElse("")
    val suffix = ninoSuffixGenerator.getOrElse("")
    s"$first$second$number$suffix"
  }

  def userInfoV1_0(): UserInfo = {
    val name = firstNameGenerator
    val lastName = lastNameGenerator
    val middleName = middleNameGenerator
    val dob = dateOfBirth
    val nino = formattedNino
    UserInfo(name, lastName, middleName, address, email(name, lastName), Some(dob), Some(nino), Some(enrolments),
             Some(government_gateway_v1_0), Some(mdtp))
  }

  def userInfoV1_1(): UserInfo = {
    val name = firstNameGenerator
    val lastName = lastNameGenerator
    val middleName = middleNameGenerator
    val dob = dateOfBirth
    val nino = formattedNino
    UserInfo(name, lastName, middleName, address, email(name, lastName), Some(dob), Some(nino), Some(enrolments),
             Some(government_gateway_v1_1), Some(mdtp))
  }
}
