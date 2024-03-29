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

package uk.gov.hmrc.cipemailfrontend.controllers

import org.mockito.ArgumentMatchersSugar.*
import org.mockito.IdiomaticMockito
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.mvc.AnyContentAsEmpty
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import play.twirl.api.Html
import uk.gov.hmrc.cipemailfrontend.views.html.LandingPage

class LandingPageControllerSpec extends AnyWordSpec
  with Matchers
  with IdiomaticMockito {

  "GET /" should {
    "return 200" in new SetUp {
      private val result = controller.landing()(fakeRequest)
      status(result) shouldBe OK

      mockLandingPage.apply(None)(*, *) was called
    }

    "return HTML" in new SetUp {
      private val result = controller.landing()(fakeRequest)
      contentType(result) shouldBe Some("text/html")
      charset(result) shouldBe Some("utf-8")

      mockLandingPage.apply(None)(*, *) was called
    }
  }

  trait SetUp {
    protected val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
    protected val mockLandingPage: LandingPage = mock[LandingPage]
    protected val controller = new LandingPageController(Helpers.stubMessagesControllerComponents(), mockLandingPage)

    mockLandingPage.apply(*)(*, *)
      .returns(Html("some html content"))
  }
}
