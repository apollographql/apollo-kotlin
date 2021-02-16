// AUTO-GENERATED FILE. DO NOT MODIFY.
//
// This class was automatically generated by Apollo GraphQL plugin from the GraphQL queries it found.
// It should not be modified by hand.
//
package com.example.named_fragment_delegate.adapter

import com.apollographql.apollo3.api.ResponseAdapterCache
import com.apollographql.apollo3.api.ResponseField
import com.apollographql.apollo3.api.internal.ListResponseAdapter
import com.apollographql.apollo3.api.internal.NullableResponseAdapter
import com.apollographql.apollo3.api.internal.ResponseAdapter
import com.apollographql.apollo3.api.internal.json.JsonReader
import com.apollographql.apollo3.api.internal.json.JsonWriter
import com.apollographql.apollo3.api.internal.stringResponseAdapter
import com.example.named_fragment_delegate.TestQuery
import com.example.named_fragment_delegate.type.CustomScalars
import kotlin.Any
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
          ResponseField.FieldSet("Droid", Hero.DroidHero.RESPONSE_FIELDS),
          ResponseField.FieldSet("Human", Hero.HumanHero.RESPONSE_FIELDS),
          ResponseField.FieldSet(null, Hero.OtherHero.RESPONSE_FIELDS),
        ),
      )
    )

    val RESPONSE_NAMES: List<String> = RESPONSE_FIELDS.map { it.responseName }
  }

  class Hero(
    customScalarAdapters: ResponseAdapterCache
  ) : ResponseAdapter<TestQuery.Data.Hero> {
    val DroidHeroAdapter: DroidHero =
        com.example.named_fragment_delegate.adapter.TestQuery_ResponseAdapter.Hero.DroidHero(customScalarAdapters)

    val HumanHeroAdapter: HumanHero =
        com.example.named_fragment_delegate.adapter.TestQuery_ResponseAdapter.Hero.HumanHero(customScalarAdapters)

    val OtherHeroAdapter: OtherHero =
        com.example.named_fragment_delegate.adapter.TestQuery_ResponseAdapter.Hero.OtherHero(customScalarAdapters)

    override fun fromResponse(reader: JsonReader): TestQuery.Data.Hero {
      reader.beginObject()
      check(reader.nextName() == "__typename")
      val typename = reader.nextString()

      return when(typename) {
        "Droid" -> DroidHeroAdapter.fromResponse(reader, typename)
        "Human" -> HumanHeroAdapter.fromResponse(reader, typename)
        else -> OtherHeroAdapter.fromResponse(reader, typename)
      }
      .also { reader.endObject() }
    }

    override fun toResponse(writer: JsonWriter, value: TestQuery.Data.Hero) {
      when(value) {
        is TestQuery.Data.Hero.DroidHero -> DroidHeroAdapter.toResponse(writer, value)
        is TestQuery.Data.Hero.HumanHero -> HumanHeroAdapter.toResponse(writer, value)
        is TestQuery.Data.Hero.OtherHero -> OtherHeroAdapter.toResponse(writer, value)
      }
    }

    class DroidHero(
      customScalarAdapters: ResponseAdapterCache
    ) {
      val stringAdapter: ResponseAdapter<String> = stringResponseAdapter

      val nullableStringAdapter: ResponseAdapter<String?> =
          NullableResponseAdapter(stringResponseAdapter)

      val nullableListOfNullableFriendsAdapter:
          ResponseAdapter<List<TestQuery.Data.Hero.DroidHero.Friends?>?> =
          NullableResponseAdapter(ListResponseAdapter(NullableResponseAdapter(Friends(customScalarAdapters))))

      fun fromResponse(reader: JsonReader, __typename: String?): TestQuery.Data.Hero.DroidHero {
        var __typename: String? = __typename
        var name: String? = null
        var primaryFunction: String? = null
        var friends: List<TestQuery.Data.Hero.DroidHero.Friends?>? = null
        while(true) {
          when (reader.selectName(RESPONSE_NAMES)) {
            0 -> __typename = stringAdapter.fromResponse(reader)
            1 -> name = stringAdapter.fromResponse(reader)
            2 -> primaryFunction = nullableStringAdapter.fromResponse(reader)
            3 -> friends = nullableListOfNullableFriendsAdapter.fromResponse(reader)
            else -> break
          }
        }
        return TestQuery.Data.Hero.DroidHero(
          __typename = __typename!!,
          name = name!!,
          primaryFunction = primaryFunction,
          friends = friends
        )
      }

      fun toResponse(writer: JsonWriter, value: TestQuery.Data.Hero.DroidHero) {
        writer.beginObject()
        writer.name("__typename")
        stringAdapter.toResponse(writer, value.__typename)
        writer.name("name")
        stringAdapter.toResponse(writer, value.name)
        writer.name("primaryFunction")
        nullableStringAdapter.toResponse(writer, value.primaryFunction)
        writer.name("friends")
        nullableListOfNullableFriendsAdapter.toResponse(writer, value.friends)
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
          ),
          ResponseField(
            type = ResponseField.Type.List(ResponseField.Type.Named.Object("Character")),
            fieldName = "friends",
            fieldSets = listOf(
              ResponseField.FieldSet(null, Friends.RESPONSE_FIELDS)
            ),
          )
        )

        val RESPONSE_NAMES: List<String> = RESPONSE_FIELDS.map { it.responseName }
      }

      class Friends(
        customScalarAdapters: ResponseAdapterCache
      ) : ResponseAdapter<TestQuery.Data.Hero.DroidHero.Friends> {
        val stringAdapter: ResponseAdapter<String> = stringResponseAdapter

        override fun fromResponse(reader: JsonReader): TestQuery.Data.Hero.DroidHero.Friends {
          var name: String? = null
          reader.beginObject()
          while(true) {
            when (reader.selectName(RESPONSE_NAMES)) {
              0 -> name = stringAdapter.fromResponse(reader)
              else -> break
            }
          }
          reader.endObject()
          return TestQuery.Data.Hero.DroidHero.Friends(
            name = name!!
          )
        }

        override fun toResponse(writer: JsonWriter, value: TestQuery.Data.Hero.DroidHero.Friends) {
          writer.beginObject()
          writer.name("name")
          stringAdapter.toResponse(writer, value.name)
          writer.endObject()
        }

        companion object {
          val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
            ResponseField(
              type = ResponseField.Type.NotNull(ResponseField.Type.Named.Other("String")),
              fieldName = "name",
            )
          )

          val RESPONSE_NAMES: List<String> = RESPONSE_FIELDS.map { it.responseName }
        }
      }
    }

    class HumanHero(
      customScalarAdapters: ResponseAdapterCache
    ) {
      val stringAdapter: ResponseAdapter<String> = stringResponseAdapter

      val uRLAdapter: ResponseAdapter<Any> =
          customScalarAdapters.responseAdapterFor<Any>(CustomScalars.URL)

      val friendsConnectionAdapter: ResponseAdapter<TestQuery.Data.Hero.HumanHero.FriendsConnection>
          = FriendsConnection(customScalarAdapters)

      fun fromResponse(reader: JsonReader, __typename: String?): TestQuery.Data.Hero.HumanHero {
        var __typename: String? = __typename
        var name: String? = null
        var profileLink: Any? = null
        var friendsConnection: TestQuery.Data.Hero.HumanHero.FriendsConnection? = null
        while(true) {
          when (reader.selectName(RESPONSE_NAMES)) {
            0 -> __typename = stringAdapter.fromResponse(reader)
            1 -> name = stringAdapter.fromResponse(reader)
            2 -> profileLink = uRLAdapter.fromResponse(reader)
            3 -> friendsConnection = friendsConnectionAdapter.fromResponse(reader)
            else -> break
          }
        }
        return TestQuery.Data.Hero.HumanHero(
          __typename = __typename!!,
          name = name!!,
          profileLink = profileLink!!,
          friendsConnection = friendsConnection!!
        )
      }

      fun toResponse(writer: JsonWriter, value: TestQuery.Data.Hero.HumanHero) {
        writer.beginObject()
        writer.name("__typename")
        stringAdapter.toResponse(writer, value.__typename)
        writer.name("name")
        stringAdapter.toResponse(writer, value.name)
        writer.name("profileLink")
        uRLAdapter.toResponse(writer, value.profileLink)
        writer.name("friendsConnection")
        friendsConnectionAdapter.toResponse(writer, value.friendsConnection)
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
            type = ResponseField.Type.NotNull(ResponseField.Type.Named.Other("URL")),
            fieldName = "profileLink",
          ),
          ResponseField(
            type = ResponseField.Type.NotNull(ResponseField.Type.Named.Object("FriendsConnection")),
            fieldName = "friendsConnection",
            fieldSets = listOf(
              ResponseField.FieldSet(null, FriendsConnection.RESPONSE_FIELDS)
            ),
          )
        )

        val RESPONSE_NAMES: List<String> = RESPONSE_FIELDS.map { it.responseName }
      }

      class FriendsConnection(
        customScalarAdapters: ResponseAdapterCache
      ) : ResponseAdapter<TestQuery.Data.Hero.HumanHero.FriendsConnection> {
        val nullableListOfNullableEdgesAdapter:
            ResponseAdapter<List<TestQuery.Data.Hero.HumanHero.FriendsConnection.Edges?>?> =
            NullableResponseAdapter(ListResponseAdapter(NullableResponseAdapter(Edges(customScalarAdapters))))

        override fun fromResponse(reader: JsonReader):
            TestQuery.Data.Hero.HumanHero.FriendsConnection {
          var edges: List<TestQuery.Data.Hero.HumanHero.FriendsConnection.Edges?>? = null
          reader.beginObject()
          while(true) {
            when (reader.selectName(RESPONSE_NAMES)) {
              0 -> edges = nullableListOfNullableEdgesAdapter.fromResponse(reader)
              else -> break
            }
          }
          reader.endObject()
          return TestQuery.Data.Hero.HumanHero.FriendsConnection(
            edges = edges
          )
        }

        override fun toResponse(writer: JsonWriter,
            value: TestQuery.Data.Hero.HumanHero.FriendsConnection) {
          writer.beginObject()
          writer.name("edges")
          nullableListOfNullableEdgesAdapter.toResponse(writer, value.edges)
          writer.endObject()
        }

        companion object {
          val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
            ResponseField(
              type = ResponseField.Type.List(ResponseField.Type.Named.Object("FriendsEdge")),
              fieldName = "edges",
              fieldSets = listOf(
                ResponseField.FieldSet(null, Edges.RESPONSE_FIELDS)
              ),
            )
          )

          val RESPONSE_NAMES: List<String> = RESPONSE_FIELDS.map { it.responseName }
        }

        class Edges(
          customScalarAdapters: ResponseAdapterCache
        ) : ResponseAdapter<TestQuery.Data.Hero.HumanHero.FriendsConnection.Edges> {
          val nullableNodeAdapter:
              ResponseAdapter<TestQuery.Data.Hero.HumanHero.FriendsConnection.Edges.Node?> =
              NullableResponseAdapter(Node(customScalarAdapters))

          override fun fromResponse(reader: JsonReader):
              TestQuery.Data.Hero.HumanHero.FriendsConnection.Edges {
            var node: TestQuery.Data.Hero.HumanHero.FriendsConnection.Edges.Node? = null
            reader.beginObject()
            while(true) {
              when (reader.selectName(RESPONSE_NAMES)) {
                0 -> node = nullableNodeAdapter.fromResponse(reader)
                else -> break
              }
            }
            reader.endObject()
            return TestQuery.Data.Hero.HumanHero.FriendsConnection.Edges(
              node = node
            )
          }

          override fun toResponse(writer: JsonWriter,
              value: TestQuery.Data.Hero.HumanHero.FriendsConnection.Edges) {
            writer.beginObject()
            writer.name("node")
            nullableNodeAdapter.toResponse(writer, value.node)
            writer.endObject()
          }

          companion object {
            val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
              ResponseField(
                type = ResponseField.Type.Named.Object("Character"),
                fieldName = "node",
                fieldSets = listOf(
                  ResponseField.FieldSet(null, Node.RESPONSE_FIELDS)
                ),
              )
            )

            val RESPONSE_NAMES: List<String> = RESPONSE_FIELDS.map { it.responseName }
          }

          class Node(
            customScalarAdapters: ResponseAdapterCache
          ) : ResponseAdapter<TestQuery.Data.Hero.HumanHero.FriendsConnection.Edges.Node> {
            val stringAdapter: ResponseAdapter<String> = stringResponseAdapter

            override fun fromResponse(reader: JsonReader):
                TestQuery.Data.Hero.HumanHero.FriendsConnection.Edges.Node {
              var name: String? = null
              reader.beginObject()
              while(true) {
                when (reader.selectName(RESPONSE_NAMES)) {
                  0 -> name = stringAdapter.fromResponse(reader)
                  else -> break
                }
              }
              reader.endObject()
              return TestQuery.Data.Hero.HumanHero.FriendsConnection.Edges.Node(
                name = name!!
              )
            }

            override fun toResponse(writer: JsonWriter,
                value: TestQuery.Data.Hero.HumanHero.FriendsConnection.Edges.Node) {
              writer.beginObject()
              writer.name("name")
              stringAdapter.toResponse(writer, value.name)
              writer.endObject()
            }

            companion object {
              val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
                ResponseField(
                  type = ResponseField.Type.NotNull(ResponseField.Type.Named.Other("String")),
                  fieldName = "name",
                )
              )

              val RESPONSE_NAMES: List<String> = RESPONSE_FIELDS.map { it.responseName }
            }
          }
        }
      }
    }

    class OtherHero(
      customScalarAdapters: ResponseAdapterCache
    ) {
      val stringAdapter: ResponseAdapter<String> = stringResponseAdapter

      fun fromResponse(reader: JsonReader, __typename: String?): TestQuery.Data.Hero.OtherHero {
        var __typename: String? = __typename
        while(true) {
          when (reader.selectName(RESPONSE_NAMES)) {
            0 -> __typename = stringAdapter.fromResponse(reader)
            else -> break
          }
        }
        return TestQuery.Data.Hero.OtherHero(
          __typename = __typename!!
        )
      }

      fun toResponse(writer: JsonWriter, value: TestQuery.Data.Hero.OtherHero) {
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
