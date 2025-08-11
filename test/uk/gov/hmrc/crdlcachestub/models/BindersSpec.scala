/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.crdlcachestub.models

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.{EitherValues, OptionValues}
import play.api.libs.json.{JsNull, JsNumber, JsString, JsTrue}

class BindersSpec
  extends AnyFlatSpec
  with Matchers
  with TableDrivenPropertyChecks
  with OptionValues
  with EitherValues {

  private val stringSetValues = Table(
    ("inputString", "setValue"),
    ("GB,XI", Set("GB", "XI")),
    ("GB", Set("GB")),
    ("", Set.empty)
  )

  private val intSetValues = Table(
    ("inputString", "isValid", "setValue"),
    ("1,2", true, Set(1, 2)),
    ("1", true, Set(1)),
    ("", true, Set.empty),
    ("A", false, null),
    ("A,B", false, null)
  )

  "Binders.bindableSet" should "bind a Set of String values from a query string" in forAll(
    stringSetValues
  ) { (inputString, setValue) =>
    val result = Binders.bindableSet[String].bind("key", Map("key" -> Seq(inputString)))
    result mustBe Some(Right(setValue))
  }

  it should "bind a Set of Int values from a query string" in forAll(
    intSetValues
  ) { (inputString, isValid, setValue) =>
    val result = Binders.bindableSet[Int].bind("key", Map("key" -> Seq(inputString)))
    if (isValid) result mustBe Some(Right(setValue))
    else result.value.left.value must startWith("Cannot parse parameter key as Int:")
  }

  it should "unbind a Set of String values into a query string as separate parameter occurrences" in forAll(
    stringSetValues
  ) { (_, setValue) =>
    val result = Binders.bindableSet.unbind("key", setValue)
    setValue.foreach { value => result must include(s"key=$value") }
  }

  it should "unbind a Set of Int values into a query string as separate parameter occurrences" in forAll(
    intSetValues
  ) { (_, isValid, setValue) =>
    whenever(isValid) {
      val result = Binders.bindableSet.unbind("key", setValue)
      setValue.foreach { value => result must include(s"key=$value") }
    }
  }

  "Binders.bindableJsValueMap" should "bind query parameters into a Map from String to JsValue, ignoring the keys parameter" in {
    Binders.bindableJsValueMap.bind(
      "",
      Map(
        // "keys" should be ignored in the output
        "keys"                         -> Seq("GB,XI"),
        "degreePlatoApplicabilityFlag" -> Seq("true"),
        "responsibleDataManager"       -> Seq("null"),
        "actionIdentification"         -> Seq("823"),
        "unitOfMeasureCode"            -> Seq("3"),
        "exciseProductCategoryCode"    -> Seq("W")
      )
    ) mustBe Some(
      Right(
        Map(
          // Boolean values should be parsed to JsBoolean
          "degreePlatoApplicabilityFlag" -> JsTrue,
          // Null values should be parsed to JsNull
          "responsibleDataManager" -> JsNull,
          // Everything else should be parsed as strings
          "actionIdentification"      -> JsString("823"),
          "unitOfMeasureCode"         -> JsString("3"),
          "exciseProductCategoryCode" -> JsString("W")
        )
      )
    )
  }

  it should "pick the first value for a query parameter when it is specified multiple times" in {
    Binders.bindableJsValueMap.bind(
      "",
      Map("exciseProductCategoryCode" -> Seq("W", "B"))
    ) mustBe Some(Right(Map("exciseProductCategoryCode" -> JsString("W"))))
  }

  it should "not support unbinding" in {
    assertThrows[UnsupportedOperationException] {
      Binders.bindableJsValueMap.unbind(
        "",
        Map(
          "degreePlatoApplicabilityFlag" -> JsTrue,
          "responsibleDataManager"       -> JsNull,
          "actionIdentification"         -> JsString("823"),
          "unitOfMeasureCode"            -> JsNumber(BigDecimal(3)),
          "exciseProductCategoryCode"    -> JsString("W")
        )
      )
    }
  }
}
