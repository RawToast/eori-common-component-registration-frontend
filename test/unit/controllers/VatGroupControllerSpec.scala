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

package unit.controllers

import common.pages.registration.VatGroupPage
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import play.api.mvc.{AnyContent, Request, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.{VatDetailsController, VatDetailsControllerOld}
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.{routes, FeatureFlags, VatGroupController}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.{SubscriptionBusinessService, SubscriptionDetailsService}
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.vat_group
import util.ControllerSpec
import util.builders.AuthBuilder.{withAuthorisedUser, withNotLoggedInUser}
import util.builders.YesNoFormBuilder.{invalidRequest, ValidRequest}
import util.builders.{AuthActionMock, SessionBuilder}

import java.util.UUID
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.global

class VatGroupControllerSpec extends ControllerSpec with BeforeAndAfterEach with AuthActionMock {

  private val yesNoInputName                  = "yes-no-answer"
  private val answerYes                       = true.toString
  private val answerNo                        = false.toString
  private val expectedYesRedirectUrl          = routes.VatGroupsCannotRegisterUsingThisServiceController.form(atarService).url
  private val expectedNoRedirectUrlOld        = VatDetailsControllerOld.createForm(atarService).url
  private val expectedNoRedirectUrl           = VatDetailsController.createForm(atarService).url
  private val mockSubscriptionDetailsService  = mock[SubscriptionDetailsService]
  private val mockSubscriptionBusinessService = mock[SubscriptionBusinessService]
  private val mockFeatureFlags                = mock[FeatureFlags]
  private val mockAuthConnector               = mock[AuthConnector]
  private val mockAuthAction                  = authAction(mockAuthConnector)

  private val vatGroupView = instanceOf[vat_group]

  private val controller =
    new VatGroupController(
      mcc,
      vatGroupView,
      mockAuthAction,
      mockSubscriptionDetailsService,
      mockSubscriptionBusinessService,
      mockFeatureFlags
    )(global)

  "Accessing the page" should {

    "reject unauthenticated users access the yes no answer form" in {
      withNotLoggedInUser(mockAuthConnector)
      val result = controller.createForm(atarService).apply(SessionBuilder.buildRequestWithSessionNoUser)
      status(result) shouldBe SEE_OTHER
    }

    "allow authenticated users access the yes no answer form" in {
      withAuthorisedUser(defaultUserId, mockAuthConnector)
      val result = controller.createForm(atarService).apply(SessionBuilder.buildRequestWithSession(defaultUserId))
      status(result) shouldBe OK
    }
  }

  "Reviewing the page" should {

    "reject unauthenticated users access the yes no answer form" in {
      withNotLoggedInUser(mockAuthConnector)
      val result = controller.reviewForm(atarService).apply(SessionBuilder.buildRequestWithSessionNoUser)
      status(result) shouldBe SEE_OTHER
    }

    "allow authenticated users access the yes no answer form" in {
      withAuthorisedUser(defaultUserId, mockAuthConnector)
      when(mockSubscriptionBusinessService.getCachedVatGroup(any[Request[AnyContent]])).thenReturn(false)
      val result = controller.reviewForm(atarService).apply(SessionBuilder.buildRequestWithSession(defaultUserId))
      status(result) shouldBe OK
    }
  }

  "submitting the form" should {

    when(mockSubscriptionDetailsService.cacheVatGroup(any())(any())).thenReturn(Future.successful())

    "ensure an option has been selected" in {
      submitForm(invalidRequest) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText(
          VatGroupPage.pageLevelErrorSummaryListXPath
        ) shouldBe VatGroupPage.problemWithSelectionError
        page.getElementsText(
          VatGroupPage.fieldLevelErrorYesNoAnswer
        ) shouldBe s"Error: ${VatGroupPage.problemWithSelectionError}"
      }
    }

    "ensure a valid option has been selected" in {
      val invalidOption = UUID.randomUUID.toString
      submitForm(ValidRequest + (yesNoInputName -> invalidOption)) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText(
          VatGroupPage.pageLevelErrorSummaryListXPath
        ) shouldBe VatGroupPage.problemWithSelectionError
        page.getElementsText(
          VatGroupPage.fieldLevelErrorYesNoAnswer
        ) shouldBe s"Error: ${VatGroupPage.problemWithSelectionError}"
      }
    }

    "redirect to Cannot Register Using This Service when 'yes' is selected" in {
      withAuthorisedUser(defaultUserId, mockAuthConnector)
      submitForm(ValidRequest + (yesNoInputName -> answerYes)) { result =>
        status(result) shouldBe SEE_OTHER
        result.header.headers(LOCATION) should endWith(expectedYesRedirectUrl)
      }
    }

    "redirect to old Vat Details form when 'no' is selected and feature flag is false" in {
      withAuthorisedUser(defaultUserId, mockAuthConnector)
      submitForm(ValidRequest + (yesNoInputName -> answerNo)) { result =>
        status(result) shouldBe SEE_OTHER
        result.header.headers(LOCATION) shouldBe expectedNoRedirectUrlOld
      }
    }

    "redirect to Vat Details form when 'no' is selected and feature flag is true" in {
      when(mockFeatureFlags.useNewVATJourney).thenReturn(true)
      withAuthorisedUser(defaultUserId, mockAuthConnector)
      submitForm(ValidRequest + (yesNoInputName -> answerNo)) { result =>
        status(result) shouldBe SEE_OTHER
        result.header.headers(LOCATION) shouldBe expectedNoRedirectUrl
      }
    }
  }

  def showForm()(test: Future[Result] => Any) {
    test(controller.createForm(atarService).apply(request = SessionBuilder.buildRequestWithSessionNoUserAndToken))
  }

  def reviewForm()(test: Future[Result] => Any) {
    test(controller.reviewForm(atarService).apply(request = SessionBuilder.buildRequestWithSessionNoUserAndToken))
  }

  def submitForm(form: Map[String, String])(test: Future[Result] => Any) {
    test(controller.submit(atarService, false).apply(SessionBuilder.buildRequestWithFormValues(form)))
  }

}
