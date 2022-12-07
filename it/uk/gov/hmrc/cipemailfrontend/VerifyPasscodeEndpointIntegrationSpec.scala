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

import org.jsoup.Jsoup
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.http.Status.OK
import play.api.libs.ws.ahc.AhcCurlRequestLogger
import uk.gov.hmrc.cipemailfrontend.utils.DataSteps

class VerifyPasscodeEndpointIntegrationSpec
  extends AnyWordSpec
    with Matchers
    with ScalaFutures
    with IntegrationPatience
    with GuiceOneServerPerSuite
    with DataSteps {

  "GET /verify/passcode" should {
    "load the verify passcode page" in {
      val response =
        wsClient
          .url(s"$baseUrl/email-example-frontend/verify/passcode?email=a@a.com")
          .withRequestFilter(AhcCurlRequestLogger())
          .get()
          .futureValue

      response.status shouldBe 200

      val document = Jsoup.parse(response.body)
      document.title() shouldBe "Enter passcode"
    }
  }

  "POST /verify/passcode" should {
    "redirect to landing page when email is verified" in {
      //generate passcode
      verify("a@a.com").futureValue

      //retrieve passcode
      val maybeEmailAndPasscode = retrievePasscode("a@a.com").futureValue

      //verify passcode (sut)
      val response =
        wsClient
          .url(s"$baseUrl/email-example-frontend/verify/passcode")
          .withRequestFilter(AhcCurlRequestLogger())
          .withFollowRedirects(false)
          .post(Map("email" -> "a@a.com", "passcode" -> s"${maybeEmailAndPasscode.get.passcode}"))
          .futureValue

      response.status shouldBe 303
      response.header("Location") shouldBe Some("/email-example-frontend?verified=true")
    }

    "return OK when email is not verified" in {
      val response =
        wsClient
          .url(s"$baseUrl/email-example-frontend/verify/passcode")
          .withRequestFilter(AhcCurlRequestLogger())
          .withFollowRedirects(false)
          .post(Map("email" -> "a@a.com", "passcode" -> "123456"))
          .futureValue

      response.status shouldBe OK
      val document = Jsoup.parse(response.body)
      document.title() shouldBe "Enter passcode"
    }

    "return 400 when form is invalid" in {
      val response =
        wsClient
          .url(s"$baseUrl/email-example-frontend/verify/passcode")
          .withRequestFilter(AhcCurlRequestLogger())
          .post(Map("email" -> "invalid"))
          .futureValue

      response.status shouldBe 400
      val document = Jsoup.parse(response.body)
      document.title() shouldBe "Enter passcode"
    }
  }
}
