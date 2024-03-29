/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.cipemailfrontend

import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.ws.WSClient
import play.api.libs.ws.ahc.AhcCurlRequestLogger
import play.api.test.Injecting

class HealthEndpointIntegrationSpec
  extends AnyWordSpec
    with Matchers
    with ScalaFutures
    with IntegrationPatience
    with GuiceOneServerPerSuite
    with Injecting {

  private val wsClient = inject[WSClient]
  private val baseUrl = s"http://localhost:$port"

  "service health endpoint" should {
    "respond with 200 status" in {
      val response =
        wsClient
          .url(s"$baseUrl/ping/ping")
          .withRequestFilter(AhcCurlRequestLogger())
          .get()
          .futureValue

      response.status shouldBe 200
    }
  }
}
