package uk.gov.hmrc.openidconnect.userinfo.services

import uk.gov.hmrc.openidconnect.userinfo.connectors.ThirdPartyDelegatedAuthorityConnector
import uk.gov.hmrc.openidconnect.userinfo.domain.{UserInfo, DesUserInfo}
import uk.gov.hmrc.play.http.{UnauthorizedException, HeaderCarrier}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

trait UserInfoTransformer {

  val countryService: CountryService
  val thirdPartyDelegatedAuthorityConnector: ThirdPartyDelegatedAuthorityConnector

  def transform(desUserInfo: DesUserInfo)(implicit hc:HeaderCarrier): Future[UserInfo] = {
    hc.authorization match {
      case Some(bearerToken) =>  thirdPartyDelegatedAuthorityConnector.fetchScopes(bearerToken.value) map { scopes =>
        UserInfo.from(desUserInfo, desUserInfo.address.countryCode flatMap countryService.getCountry)
      }
      case None => Future.failed(new UnauthorizedException("Bearer token is required"))
    }
  }

  private def transform(desUserInfo: DesUserInfo, scopes: Set[String]): UserInfo = {
    desUserInfo
    ???
  }
}

object UserInfoTransformer extends UserInfoTransformer {
  override val countryService = CountryService
  override val thirdPartyDelegatedAuthorityConnector = ThirdPartyDelegatedAuthorityConnector
}
