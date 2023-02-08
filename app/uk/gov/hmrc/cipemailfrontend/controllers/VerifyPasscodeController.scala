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

import play.api.Logging
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.cipemailfrontend.connectors.VerifyConnector
import uk.gov.hmrc.cipemailfrontend.models.{Email, EmailAndPasscode}
import uk.gov.hmrc.cipemailfrontend.views.html.{ResendPasscodePage, VerifyPasscodePage}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class VerifyPasscodeController @Inject()(
                                          mcc: MessagesControllerComponents,
                                          verifyPasscodePage: VerifyPasscodePage,
                                          resendPasscodePage: ResendPasscodePage,
                                          verifyConnector: VerifyConnector)
                                        (implicit executionContext: ExecutionContext)
  extends FrontendController(mcc)
    with Logging {

  def verifyForm(email: Option[String], requestInProgress: Boolean = false): Action[AnyContent] = Action.async { implicit request =>
    email match {
      case Some(value) => Future.successful(Ok(verifyPasscodePage(EmailAndPasscode.form.fill(EmailAndPasscode(value, "")), requestInProgress)))
      case None => Future.successful(SeeOther("/email-example-frontend"))
    }
  }

  def verify: Action[AnyContent] = Action.async { implicit request =>
    EmailAndPasscode.form.bindFromRequest().fold(
      invalid => {
        logger.warn(s"Failed to validate request")
        Future.successful(BadRequest(verifyPasscodePage(invalid, requestInProgress = true)))
      },
      emailAndPasscode => {
        verifyConnector.verifyPasscode(emailAndPasscode) map {
          case Left(l) =>
            logger.warn(l.message)
            BadRequest(verifyPasscodePage(EmailAndPasscode.form
              .withError("passcode", "verifyPasscodePage.error")
              .fill(EmailAndPasscode(emailAndPasscode.email, "")), requestInProgress = true))
          case Right(r) =>
            val optStatus = r.json \ "status"
            if (optStatus.isDefined) {
              optStatus.get.as[String] match {
                case "Verified" => SeeOther("/email-example-frontend?verified=true")
                case "Not verified" => Ok(verifyPasscodePage(EmailAndPasscode.form
                  .withError("passcode", "verifyPasscodePage.incorrectPasscode")
                  .fill(EmailAndPasscode(emailAndPasscode.email, "")), requestInProgress = true))
              }
            } else {
              Ok(verifyPasscodePage(EmailAndPasscode.form
                .withError("passcode", "verifyPasscodePage.passcodeExpired")
                .fill(EmailAndPasscode(emailAndPasscode.email, "")), requestInProgress = false))
            }
        }
      }
    )
  }

  def resendForm(email: Option[String]): Action[AnyContent] = Action.async { implicit request =>
    email match {
      case Some(value) => Future.successful(Ok(resendPasscodePage(Email.form.fill(Email(value)))))
      case None => Future.successful(SeeOther("/email-example-frontend"))
    }
  }

  def resend: Action[AnyContent] = Action.async { implicit request =>
    val email = Email.form.bindFromRequest().get
    verifyConnector.verify(Email(email.email)).map {
      case Right(_) =>
        Ok(verifyPasscodePage(EmailAndPasscode.form
          .fill(EmailAndPasscode(email.email, ""))))
      case Left(_) =>
        Ok(verifyPasscodePage(EmailAndPasscode.form.fill(EmailAndPasscode(email.email, "")), requestInProgress = true))
    }
  }
}
