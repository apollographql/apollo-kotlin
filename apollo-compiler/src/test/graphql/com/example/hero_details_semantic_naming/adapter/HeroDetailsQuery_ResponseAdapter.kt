// AUTO-GENERATED FILE. DO NOT MODIFY.
//
// This class was automatically generated by Apollo GraphQL plugin from the GraphQL queries it found.
// It should not be modified by hand.
//
package com.example.hero_details_semantic_naming.adapter

import com.apollographql.apollo3.api.ResponseAdapterCache
import com.apollographql.apollo3.api.ResponseField
import com.apollographql.apollo3.api.internal.ListResponseAdapter
import com.apollographql.apollo3.api.internal.NullableResponseAdapter
import com.apollographql.apollo3.api.internal.ResponseAdapter
import com.apollographql.apollo3.api.internal.intResponseAdapter
import com.apollographql.apollo3.api.internal.json.JsonReader
import com.apollographql.apollo3.api.internal.json.JsonWriter
import com.apollographql.apollo3.api.internal.stringResponseAdapter
import com.example.hero_details_semantic_naming.HeroDetailsQuery
import kotlin.Array
import kotlin.Int
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List

@Suppress("NAME_SHADOWING", "UNUSED_ANONYMOUS_PARAMETER", "LocalVariableName",
    "RemoveExplicitTypeArguments", "NestedLambdaShadowedImplicitParameter", "PropertyName",
    "RemoveRedundantQualifierName")
class HeroDetailsQuery_ResponseAdapter(
  customScalarAdapters: ResponseAdapterCache
) : ResponseAdapter<HeroDetailsQuery.Data> {
  val nullableHeroAdapter: ResponseAdapter<HeroDetailsQuery.Data.Hero?> =
      NullableResponseAdapter(Hero(customScalarAdapters))

  override fun fromResponse(reader: JsonReader): HeroDetailsQuery.Data {
    var hero: HeroDetailsQuery.Data.Hero? = null
    reader.beginObject()
    while(true) {
      when (reader.selectName(RESPONSE_NAMES)) {
        0 -> hero = nullableHeroAdapter.fromResponse(reader)
        else -> break
      }
    }
    reader.endObject()
    return HeroDetailsQuery.Data(
      hero = hero
    )
  }

  override fun toResponse(writer: JsonWriter, value: HeroDetailsQuery.Data) {
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
  ) : ResponseAdapter<HeroDetailsQuery.Data.Hero> {
    val stringAdapter: ResponseAdapter<String> = stringResponseAdapter

    val friendsConnectionAdapter: ResponseAdapter<HeroDetailsQuery.Data.Hero.FriendsConnection> =
        FriendsConnection(customScalarAdapters)

    override fun fromResponse(reader: JsonReader): HeroDetailsQuery.Data.Hero {
      var name: String? = null
      var friendsConnection: HeroDetailsQuery.Data.Hero.FriendsConnection? = null
      reader.beginObject()
      while(true) {
        when (reader.selectName(RESPONSE_NAMES)) {
          0 -> name = stringAdapter.fromResponse(reader)
          1 -> friendsConnection = friendsConnectionAdapter.fromResponse(reader)
          else -> break
        }
      }
      reader.endObject()
      return HeroDetailsQuery.Data.Hero(
        name = name!!,
        friendsConnection = friendsConnection!!
      )
    }

    override fun toResponse(writer: JsonWriter, value: HeroDetailsQuery.Data.Hero) {
      writer.beginObject()
      writer.name("name")
      stringAdapter.toResponse(writer, value.name)
      writer.name("friendsConnection")
      friendsConnectionAdapter.toResponse(writer, value.friendsConnection)
      writer.endObject()
    }

    companion object {
      val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
        ResponseField(
          type = ResponseField.Type.NotNull(ResponseField.Type.Named.Other("String")),
          fieldName = "name",
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
    ) : ResponseAdapter<HeroDetailsQuery.Data.Hero.FriendsConnection> {
      val nullableIntAdapter: ResponseAdapter<Int?> = NullableResponseAdapter(intResponseAdapter)

      val nullableListOfNullableEdgesAdapter:
          ResponseAdapter<List<HeroDetailsQuery.Data.Hero.FriendsConnection.Edges?>?> =
          NullableResponseAdapter(ListResponseAdapter(NullableResponseAdapter(Edges(customScalarAdapters))))

      override fun fromResponse(reader: JsonReader): HeroDetailsQuery.Data.Hero.FriendsConnection {
        var totalCount: Int? = null
        var edges: List<HeroDetailsQuery.Data.Hero.FriendsConnection.Edges?>? = null
        reader.beginObject()
        while(true) {
          when (reader.selectName(RESPONSE_NAMES)) {
            0 -> totalCount = nullableIntAdapter.fromResponse(reader)
            1 -> edges = nullableListOfNullableEdgesAdapter.fromResponse(reader)
            else -> break
          }
        }
        reader.endObject()
        return HeroDetailsQuery.Data.Hero.FriendsConnection(
          totalCount = totalCount,
          edges = edges
        )
      }

      override fun toResponse(writer: JsonWriter,
          value: HeroDetailsQuery.Data.Hero.FriendsConnection) {
        writer.beginObject()
        writer.name("totalCount")
        nullableIntAdapter.toResponse(writer, value.totalCount)
        writer.name("edges")
        nullableListOfNullableEdgesAdapter.toResponse(writer, value.edges)
        writer.endObject()
      }

      companion object {
        val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
          ResponseField(
            type = ResponseField.Type.Named.Other("Int"),
            fieldName = "totalCount",
          ),
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
      ) : ResponseAdapter<HeroDetailsQuery.Data.Hero.FriendsConnection.Edges> {
        val nullableNodeAdapter:
            ResponseAdapter<HeroDetailsQuery.Data.Hero.FriendsConnection.Edges.Node?> =
            NullableResponseAdapter(Node(customScalarAdapters))

        override fun fromResponse(reader: JsonReader):
            HeroDetailsQuery.Data.Hero.FriendsConnection.Edges {
          var node: HeroDetailsQuery.Data.Hero.FriendsConnection.Edges.Node? = null
          reader.beginObject()
          while(true) {
            when (reader.selectName(RESPONSE_NAMES)) {
              0 -> node = nullableNodeAdapter.fromResponse(reader)
              else -> break
            }
          }
          reader.endObject()
          return HeroDetailsQuery.Data.Hero.FriendsConnection.Edges(
            node = node
          )
        }

        override fun toResponse(writer: JsonWriter,
            value: HeroDetailsQuery.Data.Hero.FriendsConnection.Edges) {
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
        ) : ResponseAdapter<HeroDetailsQuery.Data.Hero.FriendsConnection.Edges.Node> {
          val stringAdapter: ResponseAdapter<String> = stringResponseAdapter

          override fun fromResponse(reader: JsonReader):
              HeroDetailsQuery.Data.Hero.FriendsConnection.Edges.Node {
            var name: String? = null
            reader.beginObject()
            while(true) {
              when (reader.selectName(RESPONSE_NAMES)) {
                0 -> name = stringAdapter.fromResponse(reader)
                else -> break
              }
            }
            reader.endObject()
            return HeroDetailsQuery.Data.Hero.FriendsConnection.Edges.Node(
              name = name!!
            )
          }

          override fun toResponse(writer: JsonWriter,
              value: HeroDetailsQuery.Data.Hero.FriendsConnection.Edges.Node) {
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
}
