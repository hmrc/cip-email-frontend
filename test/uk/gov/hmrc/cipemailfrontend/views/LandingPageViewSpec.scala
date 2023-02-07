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

package uk.gov.hmrc.cipemailfrontend.views

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.i18n.{Lang, Messages, MessagesApi, MessagesImpl}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.AnyContentAsEmpty
import play.api.test.{FakeRequest, Injecting}
import play.twirl.api.Html
import uk.gov.hmrc.cipemailfrontend.views.html.LandingPage

class LandingPageViewSpec extends AnyWordSpec
  with Matchers
  with GuiceOneAppPerSuite
  with Injecting {

  private implicit val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  private implicit val messages: Messages = MessagesImpl(Lang("en"), inject[MessagesApi])

  private val landingPageView: LandingPage = inject[LandingPage]

  override def fakeApplication(): Application = GuiceApplicationBuilder().build()

  "Viewing landing page" should {
    "display page title" in {
      val view: Html = landingPageView()
      val doc: Document = Jsoup.parse(view.body)
      doc.title() shouldBe "Email verification service"
    }

    "hide notification banner by default" in {
      val view: Html = landingPageView()
      val doc: Document = Jsoup.parse(view.body)
      doc.getElementsByClass("govuk-notification-banner").size() shouldBe 0
    }

    "show notification banner success when verified is true" in {
      val view: Html = landingPageView(verified = Some(true))
      val doc: Document = Jsoup.parse(view.body)
      doc.getElementsByClass("govuk-notification-banner--success").size() shouldBe 1
      doc.getElementsByClass("govuk-notification-banner__heading").text() shouldBe "Your email has been verified"
    }
  }
}
