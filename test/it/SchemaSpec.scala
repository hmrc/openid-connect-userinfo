package it

import java.nio.file.Paths

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.fge.jsonschema.main.JsonSchemaFactory


class SchemaSpec extends BaseFeatureSpec {
  feature("check schema and example json") {
    val validator = JsonSchemaFactory.byDefault().getValidator
    val root = System.getProperty("user.dir")
    val public10 = Paths.get(root, "public", "api", "conf", "1.0").toString
    val mapper = new ObjectMapper

    scenario("validate json") {
      val schema = mapper.readTree(Paths.get(public10, "schemas", "userinfo.json").toFile)
      val exampleJSON = mapper.readTree(Paths.get(public10, "examples", "get-user-info-example-1.json").toFile)
      val report = validator.validate(schema, exampleJSON)

      report.isSuccess shouldBe true
    }
  }
}
