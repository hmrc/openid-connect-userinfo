/*
 * Copyright 2024 HM Revenue & Customs
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

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.fge.jsonschema.core.report.LogLevel
import com.github.fge.jsonschema.main.JsonSchemaFactory
import org.scalatest.flatspec.AnyFlatSpec

import java.nio.file.Paths

class SchemaISpec extends AnyFlatSpec {
  val validator = JsonSchemaFactory.byDefault().getValidator
  val mapper = new ObjectMapper
  val schema = mapper.readTree(Paths.get(getClass.getResource("1.0/schemas/userinfo.json").toURI).toFile)
  val exampleJSON = mapper.readTree(Paths.get(getClass.getResource("1.0/examples/get-user-info-example-1.json").toURI).toFile)

  "A schema and example JSON" should "be valid" in {
    val syntaxValidator = JsonSchemaFactory.byDefault().getSyntaxValidator
    assert(syntaxValidator.schemaIsValid(schema), "Schema is NOT valid.")

    val report = validator.validate(schema, exampleJSON)
    import scala.jdk.CollectionConverters._
    assert(report.isSuccess, report.asScala.filter(_.getLogLevel == LogLevel.ERROR).map(m => m))
  }
}
