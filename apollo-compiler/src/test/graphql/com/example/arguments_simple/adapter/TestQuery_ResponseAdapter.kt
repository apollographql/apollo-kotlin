// AUTO-GENERATED FILE. DO NOT MODIFY.
//
// This class was automatically generated by Apollo GraphQL plugin from the GraphQL queries it found.
// It should not be modified by hand.
//
package com.example.arguments_simple.adapter

import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.internal.ListResponseAdapter
import com.apollographql.apollo.api.internal.NullableResponseAdapter
import com.apollographql.apollo.api.internal.ResponseAdapter
import com.apollographql.apollo.api.internal.intResponseAdapter
import com.apollographql.apollo.api.internal.json.JsonReader
import com.apollographql.apollo.api.internal.json.JsonWriter
import com.apollographql.apollo.api.internal.stringResponseAdapter
import com.apollographql.apollo.exception.UnexpectedNullValue
import com.example.arguments_simple.TestQuery
import kotlin.Array
import kotlin.Int
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List

@Suppress("NAME_SHADOWING", "UNUSED_ANONYMOUS_PARAMETER", "LocalVariableName",
    "RemoveExplicitTypeArguments", "NestedLambdaShadowedImplicitParameter", "PropertyName",
    "RemoveRedundantQualifierName")
class TestQuery_ResponseAdapter(
  customScalarAdapters: CustomScalarAdapters
) : ResponseAdapter<TestQuery.Data> {
  val heroAdapter: ResponseAdapter<TestQuery.Data.Hero?> =
      NullableResponseAdapter(Hero(customScalarAdapters))

  val heroWithReviewAdapter: ResponseAdapter<TestQuery.Data.HeroWithReview?> =
      NullableResponseAdapter(HeroWithReview(customScalarAdapters))

  override fun fromResponse(reader: JsonReader, __typename: String?): TestQuery.Data {
    var hero: TestQuery.Data.Hero? = null
    var heroWithReview: TestQuery.Data.HeroWithReview? = null
    reader.beginObject()
    while(true) {
      when (reader.selectName(RESPONSE_NAMES)) {
        0 -> hero = heroAdapter.fromResponse(reader)
        1 -> heroWithReview = heroWithReviewAdapter.fromResponse(reader)
        else -> break
      }
    }
    reader.endObject()
    return TestQuery.Data(
      hero = hero,
      heroWithReview = heroWithReview
    )
  }

  override fun toResponse(writer: JsonWriter, value: TestQuery.Data) {
    heroAdapter.toResponse(writer, value.hero)
    heroWithReviewAdapter.toResponse(writer, value.heroWithReview)
  }

  companion object {
    val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
      ResponseField(
        type = ResponseField.Type.Named.Object("Character"),
        responseName = "hero",
        fieldName = "hero",
        arguments = mapOf<String, Any?>(
          "episode" to mapOf<String, Any?>(
            "kind" to "Variable",
            "variableName" to "episode"),
          "listOfListOfStringArgs" to mapOf<String, Any?>(
            "kind" to "Variable",
            "variableName" to "listOfListOfStringArgs")),
        conditions = emptyList(),
        fieldSets = listOf(
          ResponseField.FieldSet("Droid", Hero.CharacterHero.RESPONSE_FIELDS),
          ResponseField.FieldSet("Human", Hero.CharacterHero.RESPONSE_FIELDS),
          ResponseField.FieldSet(null, Hero.OtherHero.RESPONSE_FIELDS),
        ),
      ),
      ResponseField(
        type = ResponseField.Type.Named.Object("Human"),
        responseName = "heroWithReview",
        fieldName = "heroWithReview",
        arguments = mapOf<String, Any?>(
          "episode" to mapOf<String, Any?>(
            "kind" to "Variable",
            "variableName" to "episode"),
          "review" to mapOf<String, Any?>(
            "stars" to 5,
            "favoriteColor" to mapOf<String, Any?>(
              "red" to 1,
              "blue" to 1.0),
            "listOfStringNonOptional" to emptyList<Any?>())),
        conditions = emptyList(),
        fieldSets = listOf(
          ResponseField.FieldSet(null, HeroWithReview.RESPONSE_FIELDS)
        ),
      )
    )

    val RESPONSE_NAMES: List<String> = RESPONSE_FIELDS.map { it.responseName }
  }

  class Hero(
    customScalarAdapters: CustomScalarAdapters
  ) : ResponseAdapter<TestQuery.Data.Hero> {
    val characterHeroAdapter: CharacterHero =
        com.example.arguments_simple.adapter.TestQuery_ResponseAdapter.Hero.CharacterHero(customScalarAdapters)

    val otherHeroAdapter: OtherHero =
        com.example.arguments_simple.adapter.TestQuery_ResponseAdapter.Hero.OtherHero(customScalarAdapters)

    override fun fromResponse(reader: JsonReader, __typename: String?): TestQuery.Data.Hero {
      reader.beginObject()
      check(reader.nextName() == "__typename")
      val typename = reader.nextString()

      return when(typename) {
        "Droid" -> characterHeroAdapter.fromResponse(reader, typename)
        "Human" -> characterHeroAdapter.fromResponse(reader, typename)
        else -> otherHeroAdapter.fromResponse(reader, typename)
      }
      .also { reader.endObject() }
    }

    override fun toResponse(writer: JsonWriter, value: TestQuery.Data.Hero) {
      when(value) {
        is TestQuery.Data.Hero.CharacterHero -> characterHeroAdapter.toResponse(writer, value)
        is TestQuery.Data.Hero.OtherHero -> otherHeroAdapter.toResponse(writer, value)
      }
    }

    class CharacterHero(
      customScalarAdapters: CustomScalarAdapters
    ) : ResponseAdapter<TestQuery.Data.Hero.CharacterHero> {
      val __typenameAdapter: ResponseAdapter<String> = stringResponseAdapter

      val nameAdapter: ResponseAdapter<String?> = NullableResponseAdapter(stringResponseAdapter)

      val friendsConnectionAdapter:
          ResponseAdapter<TestQuery.Data.Hero.CharacterHero.FriendsConnection> =
          FriendsConnection(customScalarAdapters)

      override fun fromResponse(reader: JsonReader, __typename: String?):
          TestQuery.Data.Hero.CharacterHero {
        var __typename: String? = __typename
        var name: String? = null
        var friendsConnection: TestQuery.Data.Hero.CharacterHero.FriendsConnection? = null
        reader.beginObject()
        while(true) {
          when (reader.selectName(RESPONSE_NAMES)) {
            0 -> __typename = __typenameAdapter.fromResponse(reader) ?: throw
                UnexpectedNullValue("__typename")
            1 -> name = nameAdapter.fromResponse(reader)
            2 -> friendsConnection = friendsConnectionAdapter.fromResponse(reader) ?: throw
                UnexpectedNullValue("friendsConnection")
            else -> break
          }
        }
        reader.endObject()
        return TestQuery.Data.Hero.CharacterHero(
          __typename = __typename!!,
          name = name,
          friendsConnection = friendsConnection!!
        )
      }

      override fun toResponse(writer: JsonWriter, value: TestQuery.Data.Hero.CharacterHero) {
        __typenameAdapter.toResponse(writer, value.__typename)
        nameAdapter.toResponse(writer, value.name)
        friendsConnectionAdapter.toResponse(writer, value.friendsConnection)
      }

      companion object {
        val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
          ResponseField(
            type = ResponseField.Type.NotNull(ResponseField.Type.Named.Other("String")),
            responseName = "__typename",
            fieldName = "__typename",
            arguments = emptyMap(),
            conditions = emptyList(),
            fieldSets = emptyList(),
          ),
          ResponseField(
            type = ResponseField.Type.Named.Other("String"),
            responseName = "name",
            fieldName = "name",
            arguments = emptyMap(),
            conditions = listOf(
              ResponseField.Condition.booleanCondition("IncludeName", false)
            ),
            fieldSets = emptyList(),
          ),
          ResponseField(
            type = ResponseField.Type.NotNull(ResponseField.Type.Named.Object("FriendsConnection")),
            responseName = "friendsConnection",
            fieldName = "friendsConnection",
            arguments = mapOf<String, Any?>(
              "first" to mapOf<String, Any?>(
                "kind" to "Variable",
                "variableName" to "friendsCount")),
            conditions = emptyList(),
            fieldSets = listOf(
              ResponseField.FieldSet(null, FriendsConnection.RESPONSE_FIELDS)
            ),
          )
        )

        val RESPONSE_NAMES: List<String> = RESPONSE_FIELDS.map { it.responseName }
      }

      class FriendsConnection(
        customScalarAdapters: CustomScalarAdapters
      ) : ResponseAdapter<TestQuery.Data.Hero.CharacterHero.FriendsConnection> {
        val totalCountAdapter: ResponseAdapter<Int?> = NullableResponseAdapter(intResponseAdapter)

        val edgesAdapter:
            ResponseAdapter<List<TestQuery.Data.Hero.CharacterHero.FriendsConnection.Edge?>?> =
            NullableResponseAdapter(ListResponseAdapter(NullableResponseAdapter(Edge(customScalarAdapters))))

        override fun fromResponse(reader: JsonReader, __typename: String?):
            TestQuery.Data.Hero.CharacterHero.FriendsConnection {
          var totalCount: Int? = null
          var edges: List<TestQuery.Data.Hero.CharacterHero.FriendsConnection.Edge?>? = null
          reader.beginObject()
          while(true) {
            when (reader.selectName(RESPONSE_NAMES)) {
              0 -> totalCount = totalCountAdapter.fromResponse(reader)
              1 -> edges = edgesAdapter.fromResponse(reader)
              else -> break
            }
          }
          reader.endObject()
          return TestQuery.Data.Hero.CharacterHero.FriendsConnection(
            totalCount = totalCount,
            edges = edges
          )
        }

        override fun toResponse(writer: JsonWriter,
            value: TestQuery.Data.Hero.CharacterHero.FriendsConnection) {
          totalCountAdapter.toResponse(writer, value.totalCount)
          edgesAdapter.toResponse(writer, value.edges)
        }

        companion object {
          val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
            ResponseField(
              type = ResponseField.Type.Named.Other("Int"),
              responseName = "totalCount",
              fieldName = "totalCount",
              arguments = emptyMap(),
              conditions = emptyList(),
              fieldSets = emptyList(),
            ),
            ResponseField(
              type = ResponseField.Type.List(ResponseField.Type.Named.Object("FriendsEdge")),
              responseName = "edges",
              fieldName = "edges",
              arguments = emptyMap(),
              conditions = emptyList(),
              fieldSets = listOf(
                ResponseField.FieldSet(null, Edge.RESPONSE_FIELDS)
              ),
            )
          )

          val RESPONSE_NAMES: List<String> = RESPONSE_FIELDS.map { it.responseName }
        }

        class Edge(
          customScalarAdapters: CustomScalarAdapters
        ) : ResponseAdapter<TestQuery.Data.Hero.CharacterHero.FriendsConnection.Edge> {
          val nodeAdapter:
              ResponseAdapter<TestQuery.Data.Hero.CharacterHero.FriendsConnection.Edge.Node?> =
              NullableResponseAdapter(Node(customScalarAdapters))

          override fun fromResponse(reader: JsonReader, __typename: String?):
              TestQuery.Data.Hero.CharacterHero.FriendsConnection.Edge {
            var node: TestQuery.Data.Hero.CharacterHero.FriendsConnection.Edge.Node? = null
            reader.beginObject()
            while(true) {
              when (reader.selectName(RESPONSE_NAMES)) {
                0 -> node = nodeAdapter.fromResponse(reader)
                else -> break
              }
            }
            reader.endObject()
            return TestQuery.Data.Hero.CharacterHero.FriendsConnection.Edge(
              node = node
            )
          }

          override fun toResponse(writer: JsonWriter,
              value: TestQuery.Data.Hero.CharacterHero.FriendsConnection.Edge) {
            nodeAdapter.toResponse(writer, value.node)
          }

          companion object {
            val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
              ResponseField(
                type = ResponseField.Type.Named.Object("Character"),
                responseName = "node",
                fieldName = "node",
                arguments = emptyMap(),
                conditions = emptyList(),
                fieldSets = listOf(
                  ResponseField.FieldSet(null, Node.RESPONSE_FIELDS)
                ),
              )
            )

            val RESPONSE_NAMES: List<String> = RESPONSE_FIELDS.map { it.responseName }
          }

          class Node(
            customScalarAdapters: CustomScalarAdapters
          ) : ResponseAdapter<TestQuery.Data.Hero.CharacterHero.FriendsConnection.Edge.Node> {
            val nameAdapter: ResponseAdapter<String?> =
                NullableResponseAdapter(stringResponseAdapter)

            override fun fromResponse(reader: JsonReader, __typename: String?):
                TestQuery.Data.Hero.CharacterHero.FriendsConnection.Edge.Node {
              var name: String? = null
              reader.beginObject()
              while(true) {
                when (reader.selectName(RESPONSE_NAMES)) {
                  0 -> name = nameAdapter.fromResponse(reader)
                  else -> break
                }
              }
              reader.endObject()
              return TestQuery.Data.Hero.CharacterHero.FriendsConnection.Edge.Node(
                name = name
              )
            }

            override fun toResponse(writer: JsonWriter,
                value: TestQuery.Data.Hero.CharacterHero.FriendsConnection.Edge.Node) {
              nameAdapter.toResponse(writer, value.name)
            }

            companion object {
              val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
                ResponseField(
                  type = ResponseField.Type.Named.Other("String"),
                  responseName = "name",
                  fieldName = "name",
                  arguments = emptyMap(),
                  conditions = listOf(
                    ResponseField.Condition.booleanCondition("IncludeName", false)
                  ),
                  fieldSets = emptyList(),
                )
              )

              val RESPONSE_NAMES: List<String> = RESPONSE_FIELDS.map { it.responseName }
            }
          }
        }
      }
    }

    class OtherHero(
      customScalarAdapters: CustomScalarAdapters
    ) : ResponseAdapter<TestQuery.Data.Hero.OtherHero> {
      val __typenameAdapter: ResponseAdapter<String> = stringResponseAdapter

      val nameAdapter: ResponseAdapter<String?> = NullableResponseAdapter(stringResponseAdapter)

      override fun fromResponse(reader: JsonReader, __typename: String?):
          TestQuery.Data.Hero.OtherHero {
        var __typename: String? = __typename
        var name: String? = null
        reader.beginObject()
        while(true) {
          when (reader.selectName(RESPONSE_NAMES)) {
            0 -> __typename = __typenameAdapter.fromResponse(reader) ?: throw
                UnexpectedNullValue("__typename")
            1 -> name = nameAdapter.fromResponse(reader)
            else -> break
          }
        }
        reader.endObject()
        return TestQuery.Data.Hero.OtherHero(
          __typename = __typename!!,
          name = name
        )
      }

      override fun toResponse(writer: JsonWriter, value: TestQuery.Data.Hero.OtherHero) {
        __typenameAdapter.toResponse(writer, value.__typename)
        nameAdapter.toResponse(writer, value.name)
      }

      companion object {
        val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
          ResponseField(
            type = ResponseField.Type.NotNull(ResponseField.Type.Named.Other("String")),
            responseName = "__typename",
            fieldName = "__typename",
            arguments = emptyMap(),
            conditions = emptyList(),
            fieldSets = emptyList(),
          ),
          ResponseField(
            type = ResponseField.Type.Named.Other("String"),
            responseName = "name",
            fieldName = "name",
            arguments = emptyMap(),
            conditions = listOf(
              ResponseField.Condition.booleanCondition("IncludeName", false)
            ),
            fieldSets = emptyList(),
          )
        )

        val RESPONSE_NAMES: List<String> = RESPONSE_FIELDS.map { it.responseName }
      }
    }
  }

  class HeroWithReview(
    customScalarAdapters: CustomScalarAdapters
  ) : ResponseAdapter<TestQuery.Data.HeroWithReview> {
    val nameAdapter: ResponseAdapter<String> = stringResponseAdapter

    override fun fromResponse(reader: JsonReader, __typename: String?):
        TestQuery.Data.HeroWithReview {
      var name: String? = null
      reader.beginObject()
      while(true) {
        when (reader.selectName(RESPONSE_NAMES)) {
          0 -> name = nameAdapter.fromResponse(reader) ?: throw UnexpectedNullValue("name")
          else -> break
        }
      }
      reader.endObject()
      return TestQuery.Data.HeroWithReview(
        name = name!!
      )
    }

    override fun toResponse(writer: JsonWriter, value: TestQuery.Data.HeroWithReview) {
      nameAdapter.toResponse(writer, value.name)
    }

    companion object {
      val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
        ResponseField(
          type = ResponseField.Type.NotNull(ResponseField.Type.Named.Other("String")),
          responseName = "name",
          fieldName = "name",
          arguments = emptyMap(),
          conditions = emptyList(),
          fieldSets = emptyList(),
        )
      )

      val RESPONSE_NAMES: List<String> = RESPONSE_FIELDS.map { it.responseName }
    }
  }
}
