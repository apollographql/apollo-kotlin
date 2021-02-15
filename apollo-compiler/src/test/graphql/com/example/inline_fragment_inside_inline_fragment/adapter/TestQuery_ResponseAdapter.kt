// AUTO-GENERATED FILE. DO NOT MODIFY.
//
// This class was automatically generated by Apollo GraphQL plugin from the GraphQL queries it found.
// It should not be modified by hand.
//
package com.example.inline_fragment_inside_inline_fragment.adapter

import com.apollographql.apollo.api.ResponseAdapterCache
import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.internal.ListResponseAdapter
import com.apollographql.apollo.api.internal.NullableResponseAdapter
import com.apollographql.apollo.api.internal.ResponseAdapter
import com.apollographql.apollo.api.internal.json.JsonReader
import com.apollographql.apollo.api.internal.json.JsonWriter
import com.apollographql.apollo.api.internal.stringResponseAdapter
import com.example.inline_fragment_inside_inline_fragment.TestQuery
import kotlin.Array
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List

@Suppress("NAME_SHADOWING", "UNUSED_ANONYMOUS_PARAMETER", "LocalVariableName",
    "RemoveExplicitTypeArguments", "NestedLambdaShadowedImplicitParameter", "PropertyName",
    "RemoveRedundantQualifierName")
class TestQuery_ResponseAdapter(
  customScalarAdapters: ResponseAdapterCache
) : ResponseAdapter<TestQuery.Data> {
  val nullableListOfNullableSearchAdapter: ResponseAdapter<List<TestQuery.Data.Search?>?> =
      NullableResponseAdapter(ListResponseAdapter(NullableResponseAdapter(Search(customScalarAdapters))))

  override fun fromResponse(reader: JsonReader): TestQuery.Data {
    var search: List<TestQuery.Data.Search?>? = null
    reader.beginObject()
    while(true) {
      when (reader.selectName(RESPONSE_NAMES)) {
        0 -> search = nullableListOfNullableSearchAdapter.fromResponse(reader)
        else -> break
      }
    }
    reader.endObject()
    return TestQuery.Data(
      search = search
    )
  }

  override fun toResponse(writer: JsonWriter, value: TestQuery.Data) {
    writer.beginObject()
    writer.name("search")
    nullableListOfNullableSearchAdapter.toResponse(writer, value.search)
    writer.endObject()
  }

  companion object {
    val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
      ResponseField(
        type = ResponseField.Type.List(ResponseField.Type.Named.Object("SearchResult")),
        fieldName = "search",
        arguments = mapOf<String, Any?>(
          "text" to "bla-bla"),
        fieldSets = listOf(
          ResponseField.FieldSet("Droid", Search.CharacterDroidSearch.RESPONSE_FIELDS),
          ResponseField.FieldSet("Human", Search.CharacterHumanSearch.RESPONSE_FIELDS),
          ResponseField.FieldSet(null, Search.OtherSearch.RESPONSE_FIELDS),
        ),
      )
    )

    val RESPONSE_NAMES: List<String> = RESPONSE_FIELDS.map { it.responseName }
  }

  class Search(
    customScalarAdapters: ResponseAdapterCache
  ) : ResponseAdapter<TestQuery.Data.Search> {
    val CharacterDroidSearchAdapter: CharacterDroidSearch =
        com.example.inline_fragment_inside_inline_fragment.adapter.TestQuery_ResponseAdapter.Search.CharacterDroidSearch(customScalarAdapters)

    val CharacterHumanSearchAdapter: CharacterHumanSearch =
        com.example.inline_fragment_inside_inline_fragment.adapter.TestQuery_ResponseAdapter.Search.CharacterHumanSearch(customScalarAdapters)

    val OtherSearchAdapter: OtherSearch =
        com.example.inline_fragment_inside_inline_fragment.adapter.TestQuery_ResponseAdapter.Search.OtherSearch(customScalarAdapters)

    override fun fromResponse(reader: JsonReader): TestQuery.Data.Search {
      reader.beginObject()
      check(reader.nextName() == "__typename")
      val typename = reader.nextString()

      return when(typename) {
        "Droid" -> CharacterDroidSearchAdapter.fromResponse(reader, typename)
        "Human" -> CharacterHumanSearchAdapter.fromResponse(reader, typename)
        else -> OtherSearchAdapter.fromResponse(reader, typename)
      }
      .also { reader.endObject() }
    }

    override fun toResponse(writer: JsonWriter, value: TestQuery.Data.Search) {
      when(value) {
        is TestQuery.Data.Search.CharacterDroidSearch -> CharacterDroidSearchAdapter.toResponse(writer, value)
        is TestQuery.Data.Search.CharacterHumanSearch -> CharacterHumanSearchAdapter.toResponse(writer, value)
        is TestQuery.Data.Search.OtherSearch -> OtherSearchAdapter.toResponse(writer, value)
      }
    }

    class CharacterDroidSearch(
      customScalarAdapters: ResponseAdapterCache
    ) {
      val stringAdapter: ResponseAdapter<String> = stringResponseAdapter

      val nullableStringAdapter: ResponseAdapter<String?> =
          NullableResponseAdapter(stringResponseAdapter)

      fun fromResponse(reader: JsonReader, __typename: String?):
          TestQuery.Data.Search.CharacterDroidSearch {
        var __typename: String? = __typename
        var name: String? = null
        var primaryFunction: String? = null
        while(true) {
          when (reader.selectName(RESPONSE_NAMES)) {
            0 -> __typename = stringAdapter.fromResponse(reader)
            1 -> name = stringAdapter.fromResponse(reader)
            2 -> primaryFunction = nullableStringAdapter.fromResponse(reader)
            else -> break
          }
        }
        return TestQuery.Data.Search.CharacterDroidSearch(
          __typename = __typename!!,
          name = name!!,
          primaryFunction = primaryFunction
        )
      }

      fun toResponse(writer: JsonWriter, value: TestQuery.Data.Search.CharacterDroidSearch) {
        writer.beginObject()
        writer.name("__typename")
        stringAdapter.toResponse(writer, value.__typename)
        writer.name("name")
        stringAdapter.toResponse(writer, value.name)
        writer.name("primaryFunction")
        nullableStringAdapter.toResponse(writer, value.primaryFunction)
        writer.endObject()
      }

      companion object {
        val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
          ResponseField.Typename,
          ResponseField(
            type = ResponseField.Type.NotNull(ResponseField.Type.Named.Other("String")),
            fieldName = "name",
          ),
          ResponseField(
            type = ResponseField.Type.Named.Other("String"),
            fieldName = "primaryFunction",
          )
        )

        val RESPONSE_NAMES: List<String> = RESPONSE_FIELDS.map { it.responseName }
      }
    }

    class CharacterHumanSearch(
      customScalarAdapters: ResponseAdapterCache
    ) {
      val stringAdapter: ResponseAdapter<String> = stringResponseAdapter

      val nullableStringAdapter: ResponseAdapter<String?> =
          NullableResponseAdapter(stringResponseAdapter)

      fun fromResponse(reader: JsonReader, __typename: String?):
          TestQuery.Data.Search.CharacterHumanSearch {
        var __typename: String? = __typename
        var name: String? = null
        var homePlanet: String? = null
        while(true) {
          when (reader.selectName(RESPONSE_NAMES)) {
            0 -> __typename = stringAdapter.fromResponse(reader)
            1 -> name = stringAdapter.fromResponse(reader)
            2 -> homePlanet = nullableStringAdapter.fromResponse(reader)
            else -> break
          }
        }
        return TestQuery.Data.Search.CharacterHumanSearch(
          __typename = __typename!!,
          name = name!!,
          homePlanet = homePlanet
        )
      }

      fun toResponse(writer: JsonWriter, value: TestQuery.Data.Search.CharacterHumanSearch) {
        writer.beginObject()
        writer.name("__typename")
        stringAdapter.toResponse(writer, value.__typename)
        writer.name("name")
        stringAdapter.toResponse(writer, value.name)
        writer.name("homePlanet")
        nullableStringAdapter.toResponse(writer, value.homePlanet)
        writer.endObject()
      }

      companion object {
        val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
          ResponseField.Typename,
          ResponseField(
            type = ResponseField.Type.NotNull(ResponseField.Type.Named.Other("String")),
            fieldName = "name",
          ),
          ResponseField(
            type = ResponseField.Type.Named.Other("String"),
            fieldName = "homePlanet",
          )
        )

        val RESPONSE_NAMES: List<String> = RESPONSE_FIELDS.map { it.responseName }
      }
    }

    class OtherSearch(
      customScalarAdapters: ResponseAdapterCache
    ) {
      val stringAdapter: ResponseAdapter<String> = stringResponseAdapter

      fun fromResponse(reader: JsonReader, __typename: String?): TestQuery.Data.Search.OtherSearch {
        var __typename: String? = __typename
        while(true) {
          when (reader.selectName(RESPONSE_NAMES)) {
            0 -> __typename = stringAdapter.fromResponse(reader)
            else -> break
          }
        }
        return TestQuery.Data.Search.OtherSearch(
          __typename = __typename!!
        )
      }

      fun toResponse(writer: JsonWriter, value: TestQuery.Data.Search.OtherSearch) {
        writer.beginObject()
        writer.name("__typename")
        stringAdapter.toResponse(writer, value.__typename)
        writer.endObject()
      }

      companion object {
        val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
          ResponseField.Typename
        )

        val RESPONSE_NAMES: List<String> = RESPONSE_FIELDS.map { it.responseName }
      }
    }
  }
}
