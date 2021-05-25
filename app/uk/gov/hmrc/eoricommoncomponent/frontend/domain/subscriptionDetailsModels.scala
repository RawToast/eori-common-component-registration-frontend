/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.eoricommoncomponent.frontend.domain

import play.api.libs.json._

abstract case class VatIdentification private[VatIdentification] (countryCode: Option[String], number: Option[String])

object VatIdentification {
  implicit val jsonFormat = Json.format[VatIdentification]

  def apply(countryCode: Option[String], number: Option[String]): VatIdentification =
    new VatIdentification(countryCode.map(_.toUpperCase), number) {}

  def apply(countryCode: String, number: String): VatIdentification =
    new VatIdentification(Option(countryCode.toUpperCase), Option(number)) {}

}

case class RegisteredContact(name: String, telephone: String, fax: Option[String], email: String)

object RegisteredContact {
  implicit val jsonFormat = Json.format[RegisteredContact]
}

case class RegisteredAddress(
  street1: String,
  street2: Option[String],
  city: String,
  postcode: Option[String],
  countryCode: String
)

object RegisteredAddress {
  implicit val jsonFormat = Json.format[RegisteredAddress]
}

case class LegalEntity(legalEntity: String)

object LegalEntity {
  implicit val jsonFormat = Json.format[LegalEntity]
}
