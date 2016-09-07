package uk.gov.hmrc.openidconnect.userinfo.connectors

import uk.gov.hmrc.openidconnect.userinfo.config.WSHttp
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.{NotFoundException, HeaderCarrier, HttpGet}
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

trait ThirdPartyDelegatedAuthorityConnector {
  val http: HttpGet
  val serviceUrl: String

  def fetchScopes(authBearerToken: String)(implicit hc: HeaderCarrier): Future[Set[String]] = {
    http.GET(s"$serviceUrl/delegated-authority?auth_bearer_token=$authBearerToken") map { response =>
      (response.json \ "token" \ "scopes").as[Set[String]]
    } recover {
      case e: NotFoundException => Set.empty
    }
  }
}

object ThirdPartyDelegatedAuthorityConnector extends ThirdPartyDelegatedAuthorityConnector with ServicesConfig {
  override val serviceUrl = baseUrl("third-party-delegated-authority")
  override val http = WSHttp
}
