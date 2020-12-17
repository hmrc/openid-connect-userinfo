/*
 * Copyright 2020 HM Revenue & Customs
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

package testSupport

import java.nio.charset.Charset

import akka.stream.Materializer
import akka.util.ByteString
import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import scala.language.postfixOps
import scala.concurrent.duration.{Duration, _}
import scala.concurrent.{Await, Future}
import scala.language.implicitConversions

trait UnitSpec extends WordSpec with Matchers {

  implicit def toFuture[A](a: A): Future[A] = Future.successful(a)

  implicit def unFuture[A](a: Future[A]): A = Await.result(a, 10 seconds)

  implicit val timeout: Duration = 1 minutes

  def await[A](future: Future[A])(implicit timeout: Duration): A = Await.result(future, timeout)

  def status(of: Result): Int = of.header.status

  def bodyOf(result: Result)(implicit mat: Materializer): String = {
    val bodyBytes: ByteString = await(result.body.consumeData)
    bodyBytes.decodeString(Charset.defaultCharset().name)
  }

  def jsonBodyOf(result: Result)(implicit mat: Materializer): JsValue = Json.parse(bodyOf(result))

}
