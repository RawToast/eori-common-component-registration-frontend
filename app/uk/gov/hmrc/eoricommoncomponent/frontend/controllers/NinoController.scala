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

package uk.gov.hmrc.eoricommoncomponent.frontend.controllers

import javax.inject.{Inject, Singleton}
import play.api.i18n.Messages
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.Individual
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{GroupId, LoggedInUserWithEnrolments}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.MatchingForms._
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.MatchingService
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.match_nino

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NinoController @Inject() (
  authAction: AuthAction,
  mcc: MessagesControllerComponents,
  matchNinoView: match_nino,
  matchingService: MatchingService
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) {

  def form(organisationType: String, service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      Future.successful(Ok(matchNinoView(ninoForm, organisationType, service)))
    }

  def submit(organisationType: String, service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => loggedInUser: LoggedInUserWithEnrolments =>
      ninoForm.bindFromRequest.fold(
        invalidForm => Future.successful(BadRequest(matchNinoView(invalidForm, organisationType, service))),
        form =>
          matchingService.matchIndividualWithNino(
            form.nino,
            Individual.withLocalDate(form.firstName, form.lastName, form.dateOfBirth),
            GroupId(loggedInUser.groupId)
          ) map {
            case true =>
              Redirect(
                uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.ConfirmContactDetailsController
                  .form(service, isInReviewMode = false)
              )
            case false =>
              val errorForm = ninoForm
                .withGlobalError(Messages("cds.matching-error.individual-not-found"))
                .fill(form)
              BadRequest(matchNinoView(errorForm, organisationType, service))
          }
      )
    }

}
