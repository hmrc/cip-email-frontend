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
import uk.gov.hmrc.cipemailfrontend.models.{Email, EmailAndPasscode}
import uk.gov.hmrc.cipemailfrontend.views.html.{ResendPasscodePage, VerifyPasscodePage}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, UpstreamErrorResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class VerifyPasscodeControllerSpec extends AnyWordSpec
  with Matchers
  with IdiomaticMockito {

  "verifyForm" should {
    "return 200" in new SetUp {
      private val result = controller.verifyForm(Some(""))(fakeRequest)
      status(result) shouldBe OK

      mockVerifyPasscodePage.apply(EmailAndPasscode.form.fill(EmailAndPasscode("", "")))(*, *) was called
    }

    "return HTML" in new SetUp {
      private val result = controller.verifyForm(Some(""))(fakeRequest)
      contentType(result) shouldBe Some("text/html")
      charset(result) shouldBe Some("utf-8")

      mockVerifyPasscodePage.apply(EmailAndPasscode.form.fill(EmailAndPasscode("", "")))(*, *) was called
    }

    "pass email to form" in new SetUp {
      private val email = "test"
      controller.verifyForm(Some(email))(fakeRequest)
      mockVerifyPasscodePage.apply(EmailAndPasscode.form.fill(EmailAndPasscode(email, "")))(*, *) was called
    }

    "redirect to landing page when email is absent" in new SetUp {
      private val result = controller.verifyForm(None)(fakeRequest)
      status(result) shouldBe SEE_OTHER
      headers(result).apply("Location") shouldBe "/email-example-frontend"

      mockVerifyPasscodePage.apply(*)(*, *) wasNever called
    }
  }

  "verify" should {
    "redirect to landing page when email passes verification" in new SetUp {
      private val email = "test"
      private val passcode = "test"
      private val request = fakeRequest.withFormUrlEncodedBody("email" -> email, "passcode" -> passcode)
      mockVerifyConnector.verifyPasscode(EmailAndPasscode(email, passcode))(any[HeaderCarrier])
        .returns(Future.successful(Right(HttpResponse(OK,
          """
          {
            "status": "Verified"
          }
          """.stripMargin))))
      private val result = controller.verify(request)
      status(result) shouldBe SEE_OTHER
      header("Location", result) shouldBe Some("/email-example-frontend?verified=true")

      mockVerifyConnector.verifyPasscode(EmailAndPasscode(email, passcode))(any[HeaderCarrier]) was called
    }

    "return OK when email fails verification" in new SetUp {
      private val email = "test"
      private val passcode = "test"
      private val request = fakeRequest.withFormUrlEncodedBody("email" -> email, "passcode" -> passcode)
      mockVerifyConnector.verifyPasscode(EmailAndPasscode(email, passcode))(any[HeaderCarrier])
        .returns(Future.successful(Right(HttpResponse(OK,
          """
          {
            "status": "Not verified"
          }
          """.stripMargin))))
      private val result = controller.verify(request)
      status(result) shouldBe OK

      mockVerifyConnector.verifyPasscode(EmailAndPasscode(email, passcode))(any[HeaderCarrier]) was called
      mockVerifyPasscodePage.apply(EmailAndPasscode.form
        .withError("passcode", "verifyPasscodePage.incorrectPasscode")
        .fill(EmailAndPasscode(email, "")), true)(*, *) was called
    }

    "return OK when passcode has expired" in new SetUp {
      private val email = "test"
      private val passcode = "test"
      private val request = fakeRequest.withFormUrlEncodedBody("email" -> email, "passcode" -> passcode)
      mockVerifyConnector.verifyPasscode(EmailAndPasscode(email, passcode))(any[HeaderCarrier])
        .returns(Future.successful(Right(HttpResponse(OK,
          """
          {
            "code": "1003",
            "message": "The passcode has expired. Request a new passcode"
          }
          """.stripMargin))))
      private val result = controller.verify(request)
      status(result) shouldBe OK

      mockVerifyConnector.verifyPasscode(EmailAndPasscode(email, passcode))(any[HeaderCarrier]) was called
      mockVerifyPasscodePage.apply(EmailAndPasscode.form
        .withError("passcode", "verifyPasscodePage.passcodeExpired")
        .fill(EmailAndPasscode(email, "")))(*, *) was called
    }

    "return bad request when form is invalid" in new SetUp {
      private val email = "test"
      private val request = fakeRequest.withFormUrlEncodedBody("email" -> email)
      private val result = controller.verify(request)
      status(result) shouldBe BAD_REQUEST
      contentAsString(result) shouldBe "some html content"

      mockVerifyPasscodePage.apply(EmailAndPasscode.form.bind(Map("email" -> email)), true)(*, *) was called
    }

    "return bad request when request is invalid" in new SetUp {
      private val email = "test"
      private val passcode = "test"
      private val request = fakeRequest.withFormUrlEncodedBody("email" -> email, "passcode" -> passcode)
      mockVerifyConnector.verifyPasscode(EmailAndPasscode(email, passcode))(any[HeaderCarrier])
        .returns(Future.successful(Left(UpstreamErrorResponse("", BAD_REQUEST))))
      private val result = controller.verify(request)
      status(result) shouldBe BAD_REQUEST
      contentAsString(result) shouldBe "some html content"

      mockVerifyConnector.verifyPasscode(EmailAndPasscode(email, passcode))(any[HeaderCarrier]) was called
      mockVerifyPasscodePage.apply(EmailAndPasscode.form
        .withError("passcode", "verifyPasscodePage.error")
        .fill(EmailAndPasscode(email, "")), true)(*, *) was called
    }
  }

  "resendForm" should {
    "return 200" in new SetUp {
      private val result = controller.resendForm(Some(""))(fakeRequest)
      status(result) shouldBe OK

      mockResendPasscodePage.apply(Email.form.fill(Email("")))(*, *) was called
    }

    "return HTML" in new SetUp {
      private val result = controller.resendForm(Some(""))(fakeRequest)
      contentType(result) shouldBe Some("text/html")
      charset(result) shouldBe Some("utf-8")

      mockResendPasscodePage.apply(Email.form.fill(Email("")))(*, *) was called
    }

    "pass email to form" in new SetUp {
      private val email = "test"
      controller.resendForm(Some(email))(fakeRequest)
      mockResendPasscodePage.apply(Email.form.fill(Email(email)))(*, *) was called
    }

    "redirect to landing page when email is absent" in new SetUp {
      private val result = controller.resendForm(None)(fakeRequest)
      status(result) shouldBe SEE_OTHER
      headers(result).apply("Location") shouldBe "/email-example-frontend"

      mockResendPasscodePage.apply(*)(*, *) wasNever called
    }
  }

  "resend" should {
    "redirect to verify passcode page" in new SetUp {
      private val email = "test"
      private val passcode = "test"
      private val request = fakeRequest.withFormUrlEncodedBody("email" -> email)
      mockVerifyConnector.verify(Email(email))(any[HeaderCarrier])
        .returns(Future.successful(Right(HttpResponse(Status.OK, "", Map("Location" -> Seq("notificationId"))))))
      private val result = controller.resend(request)
      status(result) shouldBe OK

      mockVerifyConnector.verify(Email(email))(any[HeaderCarrier]) was called

      mockVerifyPasscodePage.apply(EmailAndPasscode.form
        .fill(EmailAndPasscode(email, "")))(*, *) was called
    }
  }

  trait SetUp {
    protected val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(POST, "")
    protected val mockVerifyPasscodePage: VerifyPasscodePage = mock[VerifyPasscodePage]
    protected val mockResendPasscodePage: ResendPasscodePage = mock[ResendPasscodePage]
    protected val mockVerifyConnector: VerifyConnector = mock[VerifyConnector]

    protected val controller: VerifyPasscodeController = new VerifyPasscodeController(
      Helpers.stubMessagesControllerComponents(), mockVerifyPasscodePage, mockResendPasscodePage, mockVerifyConnector)

    mockVerifyPasscodePage.apply(*, *)(*, *)
      .returns(Html("some html content"))

    mockResendPasscodePage.apply(*)(*, *)
      .returns(Html("some html content"))
  }
}
