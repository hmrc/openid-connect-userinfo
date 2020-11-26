package unit.uk.gov.hmrc.openidconnect

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, post, stubFor, urlPathMatching}
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import org.scalatest._
import uk.gov.hmrc.integration.ServiceSpec
import scala.concurrent.duration._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

trait BaseFeatureISpec
  extends FeatureSpec
  with GivenWhenThen
  with Matchers
  with ServiceSpec
  with BeforeAndAfterEach
  with BeforeAndAfterAll {

  implicit val timeout: Duration = 1.minutes

  def await[A](future: Future[A])(implicit timeout: Duration): A = Await.result(future, timeout)

  override val externalServices: Seq[Nothing] = Seq.empty

  override lazy val additionalConfig: Map[String, Any] =
    Map[String, Any](
      "run.mode" -> "Test",
      "application.router" -> "testOnlyDoNotUseInAppConf.Routes"
    )

  val stubHost = "localhost"
  val stubPort: Int = sys.env.getOrElse("WIREMOCK_SERVICE_LOCATOR_PORT", "6008").toInt
  val wireMockServer = new WireMockServer(wireMockConfig().port(stubPort))

  wireMockServer.start()
  WireMock.configureFor(stubHost, stubPort)
  stubFor(post(urlPathMatching("/registration")).willReturn(aResponse().withStatus(204)))

}
