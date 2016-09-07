package it

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import it.stubs.DesStub
import org.scalatest._
import org.scalatestplus.play.OneServerPerSuite

import scala.concurrent.duration._

abstract class BaseFeatureSpec extends FeatureSpec with GivenWhenThen with Matchers
with BeforeAndAfterEach with BeforeAndAfterAll with OneServerPerSuite {

  override lazy val port = 19111
  val serviceUrl = s"http://localhost:$port"
  val timeout = 10.second

  val desStub = DesStub

  val mocks = Seq(desStub)

  override protected def beforeEach(): Unit = {
    mocks.foreach(m => if (!m.stub.server.isRunning) m.stub.server.start())
  }

  override protected def afterEach(): Unit = {
    mocks.foreach(_.stub.mock.resetMappings())
  }

  override protected def afterAll(): Unit = {
    mocks.foreach(_.stub.server.stop())
  }
}

case class MockHost(port: Int) {
  val server = new WireMockServer(WireMockConfiguration.wireMockConfig().port(port))
  val mock = new WireMock("localhost", port)
}

trait Stub {
  val stub: MockHost
}