/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.cipemailfrontend.connectors

import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.Configuration
import play.api.http.Status.OK
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.cipemailfrontend.config.AppConfig
import uk.gov.hmrc.cipemailfrontend.models.{Email, EmailAndPasscode}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.test.{HttpClientV2Support, WireMockSupport}

import scala.concurrent.ExecutionContext.Implicits.global

class VerifyConnectorSpec extends AnyWordSpec
  with Matchers
  with WireMockSupport
  with ScalaFutures
  with HttpClientV2Support {

  "verify" should {
    "delegate to http client" in new Setup {
      stubFor(
        post(urlEqualTo(verifyUrl))
          .willReturn(aResponse())
      )

      private val result = verifyConnector.verify(Email("test"))

      await(result).right.get.status shouldBe OK

      verify(
        postRequestedFor(urlEqualTo(verifyUrl))
          .withRequestBody(equalToJson(s"""{"email": "test"}"""))
      )
    }
  }

  "verifyPasscode" should {
    "delegate to http client" in new Setup {
      stubFor(
        post(urlEqualTo(verifyPasscodeUrl))
          .willReturn(aResponse())
      )

      private val result = verifyConnector.verifyPasscode(EmailAndPasscode("test", "test"))

      await(result).right.get.status shouldBe OK

      verify(
        postRequestedFor(urlEqualTo(verifyPasscodeUrl))
          .withRequestBody(equalToJson(s"""{"email": "test", "passcode": "test"}"""))
      )
    }
  }

  trait Setup {
    protected val verifyUrl: String = "/customer-insight-platform/email/verify"
    protected val verifyPasscodeUrl: String = s"$verifyUrl/passcode"

    private val appConfig = new AppConfig(
      Configuration.from(Map(
        "microservice.services.cipemail.host" -> wireMockHost,
        "microservice.services.cipemail.port" -> wireMockPort,
        "microservice.services.cipemail.protocol" -> "http",
        "microservice.services.cipemail.auth-token" -> "fake-token"
      ))
    )

    implicit protected val hc: HeaderCarrier = HeaderCarrier()

    protected val verifyConnector = new VerifyConnector(
      httpClientV2,
      appConfig
    )
  }
}
