package it.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import it.{MockHost, Stub}
import play.api.libs.json.Json
import uk.gov.hmrc.openidconnect.userinfo.domain.DesUserInfo

object DesStub extends Stub {
  override val stub: MockHost = new MockHost(22225)

  def willReturnUserInformation(desUserInfo: DesUserInfo) = {
    stub.mock.register(get(urlPathEqualTo(s"/pay-as-you-earn/individuals/${desUserInfo.nino}"))
      .withHeader("Authorization", equalTo("Bearer local"))
      .withHeader("Environment", equalTo("local"))
      .willReturn(aResponse().withBody(
        """
          |{}
        """.stripMargin)))
  }
}
