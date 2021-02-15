// AUTO-GENERATED FILE. DO NOT MODIFY.
//
// This class was automatically generated by Apollo GraphQL plugin from the GraphQL queries it found.
// It should not be modified by hand.
//
package com.example.directive_with_inline_fragment.adapter

import com.apollographql.apollo.api.ResponseAdapterCache
import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.internal.NullableResponseAdapter
import com.apollographql.apollo.api.internal.ResponseAdapter
import com.apollographql.apollo.api.internal.json.JsonReader
import com.apollographql.apollo.api.internal.json.JsonWriter
import com.apollographql.apollo.api.internal.stringResponseAdapter
import com.example.directive_with_inline_fragment.TestQuery
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
          ResponseField.FieldSet("Human", Hero.HumanCharacterHero.RESPONSE_FIELDS),
          ResponseField.FieldSet("Droid", Hero.DroidCharacterHero.RESPONSE_FIELDS),
          ResponseField.FieldSet(null, Hero.OtherHero.RESPONSE_FIELDS),
        ),
      )
    )

    val RESPONSE_NAMES: List<String> = RESPONSE_FIELDS.map { it.responseName }
  }

  class Hero(
    customScalarAdapters: ResponseAdapterCache
  ) : ResponseAdapter<TestQuery.Data.Hero> {
    val HumanCharacterHeroAdapter: HumanCharacterHero =
        com.example.directive_with_inline_fragment.adapter.TestQuery_ResponseAdapter.Hero.HumanCharacterHero(customScalarAdapters)

    val DroidCharacterHeroAdapter: DroidCharacterHero =
        com.example.directive_with_inline_fragment.adapter.TestQuery_ResponseAdapter.Hero.DroidCharacterHero(customScalarAdapters)

    val OtherHeroAdapter: OtherHero =
        com.example.directive_with_inline_fragment.adapter.TestQuery_ResponseAdapter.Hero.OtherHero(customScalarAdapters)

    override fun fromResponse(reader: JsonReader): TestQuery.Data.Hero {
      reader.beginObject()
      check(reader.nextName() == "__typename")
      val typename = reader.nextString()

      return when(typename) {
        "Human" -> HumanCharacterHeroAdapter.fromResponse(reader, typename)
        "Droid" -> DroidCharacterHeroAdapter.fromResponse(reader, typename)
        else -> OtherHeroAdapter.fromResponse(reader, typename)
      }
      .also { reader.endObject() }
    }

    override fun toResponse(writer: JsonWriter, value: TestQuery.Data.Hero) {
      when(value) {
        is TestQuery.Data.Hero.HumanCharacterHero -> HumanCharacterHeroAdapter.toResponse(writer, value)
        is TestQuery.Data.Hero.DroidCharacterHero -> DroidCharacterHeroAdapter.toResponse(writer, value)
        is TestQuery.Data.Hero.OtherHero -> OtherHeroAdapter.toResponse(writer, value)
      }
    }

    class HumanCharacterHero(
      customScalarAdapters: ResponseAdapterCache
    ) {
      val stringAdapter: ResponseAdapter<String> = stringResponseAdapter

      val nullableStringAdapter: ResponseAdapter<String?> =
          NullableResponseAdapter(stringResponseAdapter)

      fun fromResponse(reader: JsonReader, __typename: String?):
          TestQuery.Data.Hero.HumanCharacterHero {
        var __typename: String? = __typename
        var id: String? = null
        var name: String? = null
        var homePlanet: String? = null
        while(true) {
          when (reader.selectName(RESPONSE_NAMES)) {
            0 -> __typename = stringAdapter.fromResponse(reader)
            1 -> id = stringAdapter.fromResponse(reader)
            2 -> name = stringAdapter.fromResponse(reader)
            3 -> homePlanet = nullableStringAdapter.fromResponse(reader)
            else -> break
          }
        }
        return TestQuery.Data.Hero.HumanCharacterHero(
          __typename = __typename!!,
          id = id!!,
          name = name!!,
          homePlanet = homePlanet
        )
      }

      fun toResponse(writer: JsonWriter, value: TestQuery.Data.Hero.HumanCharacterHero) {
        writer.beginObject()
        writer.name("__typename")
        stringAdapter.toResponse(writer, value.__typename)
        writer.name("id")
        stringAdapter.toResponse(writer, value.id)
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
            fieldName = "id",
          ),
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

    class DroidCharacterHero(
      customScalarAdapters: ResponseAdapterCache
    ) {
      val stringAdapter: ResponseAdapter<String> = stringResponseAdapter

      val nullableStringAdapter: ResponseAdapter<String?> =
          NullableResponseAdapter(stringResponseAdapter)

      fun fromResponse(reader: JsonReader, __typename: String?):
          TestQuery.Data.Hero.DroidCharacterHero {
        var __typename: String? = __typename
        var id: String? = null
        var name: String? = null
        var primaryFunction: String? = null
        while(true) {
          when (reader.selectName(RESPONSE_NAMES)) {
            0 -> __typename = stringAdapter.fromResponse(reader)
            1 -> id = stringAdapter.fromResponse(reader)
            2 -> name = stringAdapter.fromResponse(reader)
            3 -> primaryFunction = nullableStringAdapter.fromResponse(reader)
            else -> break
          }
        }
        return TestQuery.Data.Hero.DroidCharacterHero(
          __typename = __typename!!,
          id = id!!,
          name = name!!,
          primaryFunction = primaryFunction
        )
      }

      fun toResponse(writer: JsonWriter, value: TestQuery.Data.Hero.DroidCharacterHero) {
        writer.beginObject()
        writer.name("__typename")
        stringAdapter.toResponse(writer, value.__typename)
        writer.name("id")
        stringAdapter.toResponse(writer, value.id)
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
            fieldName = "id",
          ),
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

    class OtherHero(
      customScalarAdapters: ResponseAdapterCache
    ) {
      val stringAdapter: ResponseAdapter<String> = stringResponseAdapter

      fun fromResponse(reader: JsonReader, __typename: String?): TestQuery.Data.Hero.OtherHero {
        var __typename: String? = __typename
        var id: String? = null
        while(true) {
          when (reader.selectName(RESPONSE_NAMES)) {
            0 -> __typename = stringAdapter.fromResponse(reader)
            1 -> id = stringAdapter.fromResponse(reader)
            else -> break
          }
        }
        return TestQuery.Data.Hero.OtherHero(
          __typename = __typename!!,
          id = id!!
        )
      }

      fun toResponse(writer: JsonWriter, value: TestQuery.Data.Hero.OtherHero) {
        writer.beginObject()
        writer.name("__typename")
        stringAdapter.toResponse(writer, value.__typename)
        writer.name("id")
        stringAdapter.toResponse(writer, value.id)
        writer.endObject()
      }

      companion object {
        val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
          ResponseField.Typename,
          ResponseField(
            type = ResponseField.Type.NotNull(ResponseField.Type.Named.Other("String")),
            fieldName = "id",
          )
        )

        val RESPONSE_NAMES: List<String> = RESPONSE_FIELDS.map { it.responseName }
      }
    }
  }
}
