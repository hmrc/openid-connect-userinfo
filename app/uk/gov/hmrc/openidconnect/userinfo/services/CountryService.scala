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

package uk.gov.hmrc.openidconnect.userinfo.services

import scala.io.Source

trait CountryService {

  val countries: Map[String, String]

  def getCountry(countryCode: Int): Option[String] = countries.get(countryCode.toString)
}

object CountryService extends CountryService {
  override val countries = loadCountriesFromFile("/resources/country.properties")

  private def loadCountriesFromFile(file: String) = {
    val is = getClass.getResourceAsStream(file)
    try {
      val lines = Source.fromInputStream(is).getLines().toSeq
      lines.map(_.split("=")).map(t => (t(0), t(1))).toMap
    } finally is.close()
  }
}
