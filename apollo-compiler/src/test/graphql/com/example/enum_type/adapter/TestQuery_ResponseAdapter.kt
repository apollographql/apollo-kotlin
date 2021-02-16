// AUTO-GENERATED FILE. DO NOT MODIFY.
//
// This class was automatically generated by Apollo GraphQL plugin from the GraphQL queries it found.
// It should not be modified by hand.
//
package com.example.enum_type.adapter

import com.apollographql.apollo3.api.ResponseAdapterCache
import com.apollographql.apollo3.api.ResponseField
import com.apollographql.apollo3.api.internal.ListResponseAdapter
import com.apollographql.apollo3.api.internal.NullableResponseAdapter
import com.apollographql.apollo3.api.internal.ResponseAdapter
import com.apollographql.apollo3.api.internal.json.JsonReader
import com.apollographql.apollo3.api.internal.json.JsonWriter
import com.apollographql.apollo3.api.internal.stringResponseAdapter
import com.example.enum_type.TestQuery
import com.example.enum_type.type.Episode
import com.example.enum_type.type.Episode_ResponseAdapter
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
  val nullableHeroAdapter: ResponseAdapter<TestQuery.Data.Hero?> =
      NullableResponseAdapter(Hero(customScalarAdapters))

  override fun fromResponse(reader: JsonReader): TestQuery.Data {
    var hero: TestQuery.Data.Hero? = null
    reader.beginObject()
    while(true) {
      when (reader.selectName(RESPONSE_NAMES)) {
        0 -> hero = nullableHeroAdapter.fromResponse(reader)
        else -> break
      }
    }
    reader.endObject()
    return TestQuery.Data(
      hero = hero
    )
  }

  override fun toResponse(writer: JsonWriter, value: TestQuery.Data) {
    writer.beginObject()
    writer.name("hero")
    nullableHeroAdapter.toResponse(writer, value.hero)
    writer.endObject()
  }

  companion object {
    val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
      ResponseField(
        type = ResponseField.Type.Named.Object("Character"),
        fieldName = "hero",
        fieldSets = listOf(
          ResponseField.FieldSet(null, Hero.RESPONSE_FIELDS)
        ),
      )
    )

    val RESPONSE_NAMES: List<String> = RESPONSE_FIELDS.map { it.responseName }
  }

  class Hero(
    customScalarAdapters: ResponseAdapterCache
  ) : ResponseAdapter<TestQuery.Data.Hero> {
    val stringAdapter: ResponseAdapter<String> = stringResponseAdapter

    val listOfNullableEpisodeAdapter: ResponseAdapter<List<Episode?>> =
        ListResponseAdapter(NullableResponseAdapter(Episode_ResponseAdapter))

    val episodeAdapter: ResponseAdapter<Episode> = Episode_ResponseAdapter

    override fun fromResponse(reader: JsonReader): TestQuery.Data.Hero {
      var name: String? = null
      var appearsIn: List<Episode?>? = null
      var firstAppearsIn: Episode? = null
      reader.beginObject()
      while(true) {
        when (reader.selectName(RESPONSE_NAMES)) {
          0 -> name = stringAdapter.fromResponse(reader)
          1 -> appearsIn = listOfNullableEpisodeAdapter.fromResponse(reader)
          2 -> firstAppearsIn = episodeAdapter.fromResponse(reader)
          else -> break
        }
      }
      reader.endObject()
      return TestQuery.Data.Hero(
        name = name!!,
        appearsIn = appearsIn!!,
        firstAppearsIn = firstAppearsIn!!
      )
    }

    override fun toResponse(writer: JsonWriter, value: TestQuery.Data.Hero) {
      writer.beginObject()
      writer.name("name")
      stringAdapter.toResponse(writer, value.name)
      writer.name("appearsIn")
      listOfNullableEpisodeAdapter.toResponse(writer, value.appearsIn)
      writer.name("firstAppearsIn")
      episodeAdapter.toResponse(writer, value.firstAppearsIn)
      writer.endObject()
    }

    companion object {
      val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
        ResponseField(
          type = ResponseField.Type.NotNull(ResponseField.Type.Named.Other("String")),
          fieldName = "name",
        ),
        ResponseField(
          type =
              ResponseField.Type.NotNull(ResponseField.Type.List(ResponseField.Type.Named.Other("Episode"))),
          fieldName = "appearsIn",
        ),
        ResponseField(
          type = ResponseField.Type.NotNull(ResponseField.Type.Named.Other("Episode")),
          fieldName = "firstAppearsIn",
        )
      )

      val RESPONSE_NAMES: List<String> = RESPONSE_FIELDS.map { it.responseName }
    }
  }
}
