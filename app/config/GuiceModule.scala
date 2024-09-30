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

package config

import com.google.inject.AbstractModule
import com.google.inject.name.Names
import com.typesafe.config.Config
import play.api.{Configuration, Environment}
import connectors.*
import services.{LiveUserInfoService, SandboxUserInfoService, UserInfoService}
import uk.gov.hmrc.play.bootstrap.config.ControllerConfig

import scala.annotation.unused

class GuiceModule(val environment: Environment, val configuration: Configuration) extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[AuthConnector]).to(classOf[AuthConnectorV1])
    bind(classOf[UserInfoService]).annotatedWith(Names.named("live")).to(classOf[LiveUserInfoService])
    bind(classOf[UserInfoService]).annotatedWith(Names.named("sandbox")).to(classOf[SandboxUserInfoService])
    bind(classOf[ControllerConfig]).toInstance {
      new ControllerConfig {
        @unused
        def controllerConfigs: Config = configuration.underlying.getConfig("controllers")
      }
    }
  }
}
