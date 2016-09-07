package unit.uk.gov.hmrc.openidconnect.userinfo.services

import org.joda.time.LocalDate
import org.mockito.BDDMockito.given
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.openidconnect.userinfo.connectors.ThirdPartyDelegatedAuthorityConnector
import uk.gov.hmrc.openidconnect.userinfo.domain._
import uk.gov.hmrc.openidconnect.userinfo.services.{CountryService, UserInfoTransformer}
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.http.logging.Authorization
import uk.gov.hmrc.play.test.UnitSpec

class UserInfoTransformerSpec extends UnitSpec with MockitoSugar {

  val ukCountryCode = 10
  val authBearerToken = "AUTH_BEARER_TOKEN"
  val desUserInfo = DesUserInfo("AB123456A", DesUserName("John", Some("A"), "Smith"), Some(LocalDate.parse("1980-01-01")),
    DesAddress("1 Station Road", "Town Centre", Some("London"), Some("England"), Some("NW1 6XE"), Some(ukCountryCode)))

  val userInfo = UserInfo(
    "John",
    "Smith",
    Some("A"),
    Address("1 Station Road\nTown Centre\nLondon\nEngland\nNW1 6XE\nUnited Kingdom", Some("NW1 6XE"), Some("United Kingdom")),
    Some(LocalDate.parse("1980-01-01")),
    "AB123456A")

  trait Setup {
    implicit val hc: HeaderCarrier = HeaderCarrier().copy(authorization = Some(Authorization(authBearerToken)))

    val transformer = new UserInfoTransformer {
      override val countryService = mock[CountryService]
      override val thirdPartyDelegatedAuthorityConnector = mock[ThirdPartyDelegatedAuthorityConnector]
    }
    given(transformer.countryService.getCountry(ukCountryCode)).willReturn(Some("United Kingdom"))
  }

  "transform" should {

    "return the full object when delegated authority has scope 'address', 'profile' and 'openid:uk-gov-identifiers'" in new Setup {

      given(transformer.thirdPartyDelegatedAuthorityConnector.fetchScopes(authBearerToken)(hc)).willReturn(Set("address", "profile", "openid:uk-gov-identifiers"))

      val result = await(transformer.transform(desUserInfo))

      result shouldBe userInfo
    }
  }
}
