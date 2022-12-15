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

import play.api.Logging
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.cipemailfrontend.connectors.VerifyConnector
import uk.gov.hmrc.cipemailfrontend.models.Email
import uk.gov.hmrc.cipemailfrontend.views.html.VerifyPage
import uk.gov.hmrc.http.HttpReads.is4xx
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class VerifyController @Inject()(
                                  mcc: MessagesControllerComponents,
                                  verifyPage: VerifyPage,
                                  verifyConnector: VerifyConnector)
                                (implicit executionContext: ExecutionContext)
  extends FrontendController(mcc)
    with Logging {

  def verifyForm(email: Option[String] = None): Action[AnyContent] = Action.async { implicit request =>
    val form = email.fold(Email.form) {
      some => Email.form.fill(Email(some))
    }
    Future.successful(Ok(verifyPage(form)))
  }

  def verify: Action[AnyContent] = Action.async { implicit request =>
    Email.form.bindFromRequest().fold(
      invalid => {
        logger.warn(s"Failed to validate request")
        Future.successful(BadRequest(verifyPage(invalid)))
      },
      email => verifyConnector.verify(email).map {
        case Right(_) => SeeOther(s"/email-example-frontend/verify/passcode?email=${email.email}")
        case Left(l) if l.statusCode == 429 => SeeOther(s"/email-example-frontend/verify/passcode?email=${email.email}&requestInProgress=true")
        case Left(l) if is4xx(l.statusCode) =>
          logger.warn(l.message)
          BadRequest(verifyPage(Email.form.withError("email", "verifyPage.error")))
      }
    )
  }
}
