/*
 * Copyright 2017 HM Revenue & Customs
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

package unit.uk.gov.hmrc.openidconnect.userinfo.services

import uk.gov.hmrc.openidconnect.userinfo.services.CountryService
import uk.gov.hmrc.play.test.UnitSpec

class CountryServiceSpec extends UnitSpec {

  "getCountry" should {
    "return the country when the country is in the file" in {

      CountryService.getCountry(1) shouldBe Some("GREAT BRITAIN")
      CountryService.getCountry(283) shouldBe Some("REPUBLIC OF MONTENEGRO")
    }

    "return None when the country is not in the file" in {

      CountryService.getCountry(284) shouldBe None
    }
  }
}
