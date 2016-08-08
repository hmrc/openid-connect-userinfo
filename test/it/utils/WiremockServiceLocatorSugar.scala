package it.utils

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import play.api.libs.json.Json
import uk.gov.hmrc.api.domain.Registration

trait WiremockServiceLocatorSugar {
  lazy val wireMockUrl = s"http://$stubHost:$stubPort"
  lazy val wireMockServer = new WireMockServer(wireMockConfig().port(stubPort))
  val stubPort = sys.env.getOrElse("WIREMOCK_SERVICE_LOCATOR_PORT", "11112").toInt
  val stubHost = "localhost"

  def regPayloadStringFor(serviceName: String, serviceUrl: String): String =
    Json.toJson(Registration(serviceName, serviceUrl, Some(Map("third-party-api" -> "true")))).toString

  def startMockServer() = {
    wireMockServer.start()
    WireMock.configureFor(stubHost, stubPort)
  }

  def stopMockServer() = {
    wireMockServer.stop()
    // A cleaner solution to reset the mappings, but only works with wiremock "1.57" (at the moment version 1.48 is pulled)
    //wireMockServer.resetMappings()
  }

  def stubRegisterEndpoint(status: Int) = stubFor(post(urlMatching("/registration")).willReturn(aResponse().withStatus(status)))
}
