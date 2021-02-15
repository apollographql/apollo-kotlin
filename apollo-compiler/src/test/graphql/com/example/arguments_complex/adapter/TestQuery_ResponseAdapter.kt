// AUTO-GENERATED FILE. DO NOT MODIFY.
//
// This class was automatically generated by Apollo GraphQL plugin from the GraphQL queries it found.
// It should not be modified by hand.
//
package com.example.arguments_complex.adapter

import com.apollographql.apollo.api.ResponseAdapterCache
import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.internal.NullableResponseAdapter
import com.apollographql.apollo.api.internal.ResponseAdapter
import com.apollographql.apollo.api.internal.doubleResponseAdapter
import com.apollographql.apollo.api.internal.json.JsonReader
import com.apollographql.apollo.api.internal.json.JsonWriter
import com.apollographql.apollo.api.internal.stringResponseAdapter
import com.example.arguments_complex.TestQuery
import kotlin.Array
import kotlin.Double
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List

@Suppress("NAME_SHADOWING", "UNUSED_ANONYMOUS_PARAMETER", "LocalVariableName",
    "RemoveExplicitTypeArguments", "NestedLambdaShadowedImplicitParameter", "PropertyName",
    "RemoveRedundantQualifierName")
class TestQuery_ResponseAdapter(
  customScalarAdapters: ResponseAdapterCache
) : ResponseAdapter<TestQuery.Data> {
  val nullableHeroWithReviewAdapter: ResponseAdapter<TestQuery.Data.HeroWithReview?> =
      NullableResponseAdapter(HeroWithReview(customScalarAdapters))

  override fun fromResponse(reader: JsonReader): TestQuery.Data {
    var heroWithReview: TestQuery.Data.HeroWithReview? = null
    reader.beginObject()
    while(true) {
      when (reader.selectName(RESPONSE_NAMES)) {
        0 -> heroWithReview = nullableHeroWithReviewAdapter.fromResponse(reader)
        else -> break
      }
    }
    reader.endObject()
    return TestQuery.Data(
      heroWithReview = heroWithReview
    )
  }

  override fun toResponse(writer: JsonWriter, value: TestQuery.Data) {
    writer.beginObject()
    writer.name("heroWithReview")
    nullableHeroWithReviewAdapter.toResponse(writer, value.heroWithReview)
    writer.endObject()
  }

  companion object {
    val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
      ResponseField(
        type = ResponseField.Type.Named.Object("Human"),
        fieldName = "heroWithReview",
        arguments = mapOf<String, Any?>(
          "episode" to mapOf<String, Any?>(
            "kind" to "Variable",
            "variableName" to "episode"),
          "review" to mapOf<String, Any?>(
            "stars" to mapOf<String, Any?>(
              "kind" to "Variable",
              "variableName" to "stars"),
            "favoriteColor" to mapOf<String, Any?>(
              "red" to 0,
              "green" to mapOf<String, Any?>(
                "kind" to "Variable",
                "variableName" to "greenValue"),
              "blue" to 0.0),
            "booleanNonOptional" to false,
            "listOfStringNonOptional" to emptyList<Any?>()),
          "listOfInts" to listOf<Any?>(
            mapOf<String, Any?>(
              "kind" to "Variable",
              "variableName" to "stars"),
            mapOf<String, Any?>(
              "kind" to "Variable",
              "variableName" to "stars"))),
        fieldSets = listOf(
          ResponseField.FieldSet(null, HeroWithReview.RESPONSE_FIELDS)
        ),
      )
    )

    val RESPONSE_NAMES: List<String> = RESPONSE_FIELDS.map { it.responseName }
  }

  class HeroWithReview(
    customScalarAdapters: ResponseAdapterCache
  ) : ResponseAdapter<TestQuery.Data.HeroWithReview> {
    val stringAdapter: ResponseAdapter<String> = stringResponseAdapter

    val nullableFloatAdapter: ResponseAdapter<Double?> =
        NullableResponseAdapter(doubleResponseAdapter)

    override fun fromResponse(reader: JsonReader): TestQuery.Data.HeroWithReview {
      var name: String? = null
      var height: Double? = null
      reader.beginObject()
      while(true) {
        when (reader.selectName(RESPONSE_NAMES)) {
          0 -> name = stringAdapter.fromResponse(reader)
          1 -> height = nullableFloatAdapter.fromResponse(reader)
          else -> break
        }
      }
      reader.endObject()
      return TestQuery.Data.HeroWithReview(
        name = name!!,
        height = height
      )
    }

    override fun toResponse(writer: JsonWriter, value: TestQuery.Data.HeroWithReview) {
      writer.beginObject()
      writer.name("name")
      stringAdapter.toResponse(writer, value.name)
      writer.name("height")
      nullableFloatAdapter.toResponse(writer, value.height)
      writer.endObject()
    }

    companion object {
      val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
        ResponseField(
          type = ResponseField.Type.NotNull(ResponseField.Type.Named.Other("String")),
          fieldName = "name",
        ),
        ResponseField(
          type = ResponseField.Type.Named.Other("Float"),
          fieldName = "height",
          arguments = mapOf<String, Any?>(
            "unit" to "FOOT"),
        )
      )

      val RESPONSE_NAMES: List<String> = RESPONSE_FIELDS.map { it.responseName }
    }
  }
}
