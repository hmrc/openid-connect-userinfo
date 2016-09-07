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

package uk.gov.hmrc.openidconnect.userinfo.data

import uk.gov.hmrc.openidconnect.userinfo.domain.{Address, UserInfo}
import org.joda.time._
import org.scalacheck.Gen

trait UserInfoGenerator {
  val firstNames = List("Roland", "Eddie", "Susanna", "Jake", "Oy", "Cuthbert", "Alain", "Jamie", "Thomas", "Susan", "Randall")
  val middleNames = List(Some("De"), Some("Donald"), Some("Billy"), Some("E"), Some("Alex"), Some("Abel"), None, None, None, None, None)
  val lastNames = List("Deschain", "Dean", "Dean", "Chambers", "Bumbler", "Allgood", "Johns", "Curry", "Whitman", "Delgado", "Flagg")
  val address = Address(
    """221B Baker Street
      |London
      |NW1 9NT
      |Great Britain""".stripMargin, Some("NW1 9NT"), Some("Great Britain"))
  private lazy val ninoPrefixes = "ABCEGHJKLMNPRSTWXYZ"
  private lazy val ninoSuffixes = "ABCD"

  private val nameGen = Gen.oneOf(firstNames)
  private val lastNameGen = Gen.oneOf(lastNames)
  private val middleNameGen = Gen.oneOf(middleNames)
  private val dayGen = Gen.choose(1, 28)
  private val monthGen = Gen.choose(1, 12)
  private val yearGen = Gen.choose(1940, 1998)
  private val prefixGen = Gen.oneOf(ninoPrefixes)
  private val suffixGen = Gen.oneOf(ninoSuffixes)
  private val numbersGen = Gen.choose(100000, 999999)

  private def dateOfBirth = {
    for {
      day <- dayGen
      month <- monthGen
      year <- yearGen
    } yield new LocalDate(year, month, day)
  }

  private def formattedNino = {
    for {
      first <- prefixGen
      second <- prefixGen
      number <- numbersGen
      suffix <- suffixGen
    } yield s"$first$second$number$suffix"
  }

  val userInfo = for {
    name <- nameGen
    lastName <- lastNameGen
    middleName <- middleNameGen
    dob <- dateOfBirth
    nino <- formattedNino
  } yield UserInfo(name, lastName, middleName, address, Some(dob), nino)
}

object UserInfoGenerator extends UserInfoGenerator
