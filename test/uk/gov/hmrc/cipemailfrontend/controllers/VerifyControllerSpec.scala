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

package uk.gov.hmrc.cipemailfrontend.controllers

import org.mockito.ArgumentMatchersSugar.{*, any}
import org.mockito.IdiomaticMockito
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.Status
import play.api.mvc.AnyContentAsEmpty
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import play.twirl.api.Html
import uk.gov.hmrc.cipemailfrontend.connectors.VerifyConnector
import uk.gov.hmrc.cipemailfrontend.models.Email
import uk.gov.hmrc.cipemailfrontend.views.html.VerifyPage
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, UpstreamErrorResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class VerifyControllerSpec extends AnyWordSpec
  with Matchers
  with IdiomaticMockito {

  "verifyForm" should {
    "return 200" in new SetUp {
      private val result = controller.verifyForm()(fakeRequest)
      status(result) shouldBe OK

      mockVerifyPage.apply(Email.form)(*, *) was called
    }

    "return HTML" in new SetUp {
      private val result = controller.verifyForm()(fakeRequest)
      contentType(result) shouldBe Some("text/html")
      charset(result) shouldBe Some("utf-8")

      mockVerifyPage.apply(Email.form)(*, *) was called
    }

    "load empty form by default" in new SetUp {
      controller.verifyForm()(fakeRequest)
      mockVerifyPage.apply(Email.form)(*, *) was called
    }

    "load form with email when supplied" in new SetUp {
      private val email = "test"
      controller.verifyForm(Some(email))(fakeRequest)
      mockVerifyPage.apply(Email.form.fill(Email(email)))(*, *) was called
    }
  }

  "verify" should {
    "redirect to verify passcode when request is valid" in new SetUp {
      private val email = "test"
      private val request = fakeRequest.withFormUrlEncodedBody("email" -> email)
      mockVerifyConnector.verify(Email(email))(any[HeaderCarrier])
        .returns(Future.successful(Right(HttpResponse(Status.OK, "", Map("Location" -> Seq("notificationId"))))))
      private val result = controller.verify(request)
      status(result) shouldBe SEE_OTHER
      header("Location", result) shouldBe Some(s"/email-example-frontend/verify/passcode?email=$email")

      mockVerifyConnector.verify(Email(email))(any[HeaderCarrier]) was called
    }

    "return bad request when form is invalid" in new SetUp {
      private val result = controller.verify(fakeRequest)
      status(result) shouldBe BAD_REQUEST
      contentAsString(result) shouldBe "some html content"

      mockVerifyPage.apply(Email.form.withError("email", "error.required"))(*, *) was called
    }

    "return bad request when request is invalid" in new SetUp {
      private val email = "test"
      private val request = fakeRequest.withFormUrlEncodedBody("email" -> email)
      mockVerifyConnector.verify(Email(email))(any[HeaderCarrier])
        .returns(Future.successful(Left(UpstreamErrorResponse("", Status.BAD_REQUEST))))
      private val result = controller.verify(request)
      status(result) shouldBe BAD_REQUEST
      contentAsString(result) shouldBe "some html content"

      mockVerifyConnector.verify(Email(email))(any[HeaderCarrier]) was called
      mockVerifyPage.apply(Email.form.withError("email", "verifyPage.error"))(*, *) was called
    }
  }

  trait SetUp {
    protected val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(POST, "")
    protected val mockVerifyPage: VerifyPage = mock[VerifyPage]
    protected val mockVerifyConnector: VerifyConnector = mock[VerifyConnector]

    protected val controller: VerifyController = new VerifyController(Helpers.stubMessagesControllerComponents(), mockVerifyPage, mockVerifyConnector)

    mockVerifyPage.apply(*)(*, *)
      .returns(Html("some html content"))
  }
}
