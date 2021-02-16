// AUTO-GENERATED FILE. DO NOT MODIFY.
//
// This class was automatically generated by Apollo GraphQL plugin from the GraphQL queries it found.
// It should not be modified by hand.
//
package com.example.directives.adapter

import com.apollographql.apollo3.api.ResponseAdapterCache
import com.apollographql.apollo3.api.ResponseField
import com.apollographql.apollo3.api.internal.NullableResponseAdapter
import com.apollographql.apollo3.api.internal.ResponseAdapter
import com.apollographql.apollo3.api.internal.intResponseAdapter
import com.apollographql.apollo3.api.internal.json.JsonReader
import com.apollographql.apollo3.api.internal.json.JsonWriter
import com.apollographql.apollo3.api.internal.stringResponseAdapter
import com.example.directives.TestQuery
import kotlin.Array
import kotlin.Int
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
    val nullableStringAdapter: ResponseAdapter<String?> =
        NullableResponseAdapter(stringResponseAdapter)

    val nullableFriendsConnectionAdapter: ResponseAdapter<TestQuery.Data.Hero.FriendsConnection?> =
        NullableResponseAdapter(FriendsConnection(customScalarAdapters))

    override fun fromResponse(reader: JsonReader): TestQuery.Data.Hero {
      var name: String? = null
      var friendsConnection: TestQuery.Data.Hero.FriendsConnection? = null
      reader.beginObject()
      while(true) {
        when (reader.selectName(RESPONSE_NAMES)) {
          0 -> name = nullableStringAdapter.fromResponse(reader)
          1 -> friendsConnection = nullableFriendsConnectionAdapter.fromResponse(reader)
          else -> break
        }
      }
      reader.endObject()
      return TestQuery.Data.Hero(
        name = name,
        friendsConnection = friendsConnection
      )
    }

    override fun toResponse(writer: JsonWriter, value: TestQuery.Data.Hero) {
      writer.beginObject()
      writer.name("name")
      nullableStringAdapter.toResponse(writer, value.name)
      writer.name("friendsConnection")
      nullableFriendsConnectionAdapter.toResponse(writer, value.friendsConnection)
      writer.endObject()
    }

    companion object {
      val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
        ResponseField(
          type = ResponseField.Type.Named.Other("String"),
          fieldName = "name",
          conditions = listOf(
            ResponseField.Condition.booleanCondition("includeName", false)
          ),
        ),
        ResponseField(
          type = ResponseField.Type.Named.Object("FriendsConnection"),
          fieldName = "friendsConnection",
          conditions = listOf(
            ResponseField.Condition.booleanCondition("skipFriends", true)
          ),
          fieldSets = listOf(
            ResponseField.FieldSet(null, FriendsConnection.RESPONSE_FIELDS)
          ),
        )
      )

      val RESPONSE_NAMES: List<String> = RESPONSE_FIELDS.map { it.responseName }
    }

    class FriendsConnection(
      customScalarAdapters: ResponseAdapterCache
    ) : ResponseAdapter<TestQuery.Data.Hero.FriendsConnection> {
      val nullableIntAdapter: ResponseAdapter<Int?> = NullableResponseAdapter(intResponseAdapter)

      override fun fromResponse(reader: JsonReader): TestQuery.Data.Hero.FriendsConnection {
        var totalCount: Int? = null
        reader.beginObject()
        while(true) {
          when (reader.selectName(RESPONSE_NAMES)) {
            0 -> totalCount = nullableIntAdapter.fromResponse(reader)
            else -> break
          }
        }
        reader.endObject()
        return TestQuery.Data.Hero.FriendsConnection(
          totalCount = totalCount
        )
      }

      override fun toResponse(writer: JsonWriter, value: TestQuery.Data.Hero.FriendsConnection) {
        writer.beginObject()
        writer.name("totalCount")
        nullableIntAdapter.toResponse(writer, value.totalCount)
        writer.endObject()
      }

      companion object {
        val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
          ResponseField(
            type = ResponseField.Type.Named.Other("Int"),
            fieldName = "totalCount",
          )
        )

        val RESPONSE_NAMES: List<String> = RESPONSE_FIELDS.map { it.responseName }
      }
    }
  }
}
