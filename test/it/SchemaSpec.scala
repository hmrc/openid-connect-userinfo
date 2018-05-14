/*
 * Copyright 2018 HM Revenue & Customs
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

package it

import java.nio.file.Paths

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.fge.jsonschema.core.report.LogLevel
import com.github.fge.jsonschema.main.JsonSchemaFactory
import org.scalatest.FlatSpec

class SchemaSpec extends FlatSpec {
  val validator = JsonSchemaFactory.byDefault().getValidator
  val root = System.getProperty("user.dir")
  val public10 = Paths.get(root, "public", "api", "conf", "1.0").toString
  val mapper = new ObjectMapper

  val schema = mapper.readTree(Paths.get(public10, "schemas", "userinfo.json").toFile)
  val exampleJSON = mapper.readTree(Paths.get(public10, "examples", "get-user-info-example-1.json").toFile)

  "A schema and example JSON" should "be valid" in {
    val syntaxValidator = JsonSchemaFactory.byDefault().getSyntaxValidator
    assert(syntaxValidator.schemaIsValid(schema), "Schema is NOT valid.")

    val report = validator.validate(schema, exampleJSON)
    import scala.collection.JavaConversions._
    assert(report.isSuccess, report.filter(_.getLogLevel == LogLevel.ERROR).map(m => m))
  }
}
