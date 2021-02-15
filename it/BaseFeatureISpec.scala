import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, post, stubFor, urlPathMatching}
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest._
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.integration.ServiceSpec

import scala.concurrent.duration.{Duration, _}
import scala.concurrent.{Await, Future}
import scala.collection.JavaConverters._

import scala.util.Try

trait BaseFeatureISpec
  extends FeatureSpec
    with GivenWhenThen
    with Matchers
    with GuiceOneServerPerSuite
    with BeforeAndAfterEach
    with BeforeAndAfterAll {

  implicit val timeout: Duration = 1.minutes

  override def fakeApplication(): Application = GuiceApplicationBuilder().configure(
    flatStructurePortMapping()
      ++ extraConfig
  ).build()

  protected def resource(resource: String) = s"http://localhost:$port$resource"

  def await[A](future: Future[A])(implicit timeout: Duration): A = Await.result(future, timeout)

  private def flatStructurePortMapping(): Map[String, Int] = {
    lazy val config: Config = ConfigFactory.load()
    Try(config.getConfig("microservice.services"))
      .getOrElse(ConfigFactory.empty())
      .entrySet()
      .asScala
      .collect {
        case e if e.getKey.endsWith("port") =>
          "microservice.services." + e.getKey -> stubPort
      }.toMap

  }

  def extraConfig: Map[String, Any] = Map[String, Any](
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
