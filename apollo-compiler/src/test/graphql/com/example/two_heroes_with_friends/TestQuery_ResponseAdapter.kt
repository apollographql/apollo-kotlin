// AUTO-GENERATED FILE. DO NOT MODIFY.
//
// This class was automatically generated by Apollo GraphQL plugin from the GraphQL queries it found.
// It should not be modified by hand.
//
package com.example.two_heroes_with_friends

import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.internal.ResponseAdapter
import com.apollographql.apollo.api.internal.ResponseReader
import com.apollographql.apollo.api.internal.ResponseWriter
import com.example.two_heroes_with_friends.type.CustomType
import kotlin.Array
import kotlin.Int
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List

@Suppress("NAME_SHADOWING", "UNUSED_ANONYMOUS_PARAMETER", "LocalVariableName",
    "RemoveExplicitTypeArguments", "NestedLambdaShadowedImplicitParameter", "PropertyName",
    "RemoveRedundantQualifierName")
object TestQuery_ResponseAdapter : ResponseAdapter<TestQuery.Data> {
  private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
    ResponseField.forObject("r2", "hero", null, true, null),
    ResponseField.forObject("luke", "hero", mapOf<String, Any>(
      "episode" to "EMPIRE"), true, null)
  )

  override fun fromResponse(reader: ResponseReader, __typename: String?): TestQuery.Data {
    return reader.run {
      var r2: TestQuery.R2? = null
      var luke: TestQuery.Luke? = null
      while(true) {
        when (selectField(RESPONSE_FIELDS)) {
          0 -> r2 = readObject<TestQuery.R2>(RESPONSE_FIELDS[0]) { reader ->
            TestQuery_ResponseAdapter.R2_ResponseAdapter.fromResponse(reader)
          }
          1 -> luke = readObject<TestQuery.Luke>(RESPONSE_FIELDS[1]) { reader ->
            TestQuery_ResponseAdapter.Luke_ResponseAdapter.fromResponse(reader)
          }
          else -> break
        }
      }
      TestQuery.Data(
        r2 = r2,
        luke = luke
      )
    }
  }

  override fun toResponse(writer: ResponseWriter, value: TestQuery.Data) {
    if(value.r2 == null) {
      writer.writeObject(RESPONSE_FIELDS[0], null)
    } else {
      writer.writeObject(RESPONSE_FIELDS[0]) { writer ->
        TestQuery_ResponseAdapter.R2_ResponseAdapter.toResponse(writer, value.r2)
      }
    }
    if(value.luke == null) {
      writer.writeObject(RESPONSE_FIELDS[1], null)
    } else {
      writer.writeObject(RESPONSE_FIELDS[1]) { writer ->
        TestQuery_ResponseAdapter.Luke_ResponseAdapter.toResponse(writer, value.luke)
      }
    }
  }

  object Node_ResponseAdapter : ResponseAdapter<TestQuery.Node> {
    private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
      ResponseField.forString("name", "name", null, false, null)
    )

    override fun fromResponse(reader: ResponseReader, __typename: String?): TestQuery.Node {
      return reader.run {
        var name: String? = null
        while(true) {
          when (selectField(RESPONSE_FIELDS)) {
            0 -> name = readString(RESPONSE_FIELDS[0])
            else -> break
          }
        }
        TestQuery.Node(
          name = name!!
        )
      }
    }

    override fun toResponse(writer: ResponseWriter, value: TestQuery.Node) {
      writer.writeString(RESPONSE_FIELDS[0], value.name)
    }
  }

  object Edge_ResponseAdapter : ResponseAdapter<TestQuery.Edge> {
    private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
      ResponseField.forObject("node", "node", null, true, null)
    )

    override fun fromResponse(reader: ResponseReader, __typename: String?): TestQuery.Edge {
      return reader.run {
        var node: TestQuery.Node? = null
        while(true) {
          when (selectField(RESPONSE_FIELDS)) {
            0 -> node = readObject<TestQuery.Node>(RESPONSE_FIELDS[0]) { reader ->
              TestQuery_ResponseAdapter.Node_ResponseAdapter.fromResponse(reader)
            }
            else -> break
          }
        }
        TestQuery.Edge(
          node = node
        )
      }
    }

    override fun toResponse(writer: ResponseWriter, value: TestQuery.Edge) {
      if(value.node == null) {
        writer.writeObject(RESPONSE_FIELDS[0], null)
      } else {
        writer.writeObject(RESPONSE_FIELDS[0]) { writer ->
          TestQuery_ResponseAdapter.Node_ResponseAdapter.toResponse(writer, value.node)
        }
      }
    }
  }

  object FriendsConnection_ResponseAdapter : ResponseAdapter<TestQuery.FriendsConnection> {
    private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
      ResponseField.forInt("totalCount", "totalCount", null, true, null),
      ResponseField.forList("edges", "edges", null, true, null)
    )

    override fun fromResponse(reader: ResponseReader, __typename: String?):
        TestQuery.FriendsConnection {
      return reader.run {
        var totalCount: Int? = null
        var edges: List<TestQuery.Edge?>? = null
        while(true) {
          when (selectField(RESPONSE_FIELDS)) {
            0 -> totalCount = readInt(RESPONSE_FIELDS[0])
            1 -> edges = readList<TestQuery.Edge>(RESPONSE_FIELDS[1]) { reader ->
              reader.readObject<TestQuery.Edge> { reader ->
                TestQuery_ResponseAdapter.Edge_ResponseAdapter.fromResponse(reader)
              }
            }
            else -> break
          }
        }
        TestQuery.FriendsConnection(
          totalCount = totalCount,
          edges = edges
        )
      }
    }

    override fun toResponse(writer: ResponseWriter, value: TestQuery.FriendsConnection) {
      writer.writeInt(RESPONSE_FIELDS[0], value.totalCount)
      writer.writeList(RESPONSE_FIELDS[1], value.edges) { values, listItemWriter ->
        values?.forEach { value ->
          if(value == null) {
            listItemWriter.writeObject(null)
          } else {
            listItemWriter.writeObject { writer ->
              TestQuery_ResponseAdapter.Edge_ResponseAdapter.toResponse(writer, value)
            }
          }
        }
      }
    }
  }

  object R2_ResponseAdapter : ResponseAdapter<TestQuery.R2> {
    private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
      ResponseField.forString("name", "name", null, false, null),
      ResponseField.forObject("friendsConnection", "friendsConnection", null, false, null)
    )

    override fun fromResponse(reader: ResponseReader, __typename: String?): TestQuery.R2 {
      return reader.run {
        var name: String? = null
        var friendsConnection: TestQuery.FriendsConnection? = null
        while(true) {
          when (selectField(RESPONSE_FIELDS)) {
            0 -> name = readString(RESPONSE_FIELDS[0])
            1 -> friendsConnection = readObject<TestQuery.FriendsConnection>(RESPONSE_FIELDS[1]) { reader ->
              TestQuery_ResponseAdapter.FriendsConnection_ResponseAdapter.fromResponse(reader)
            }
            else -> break
          }
        }
        TestQuery.R2(
          name = name!!,
          friendsConnection = friendsConnection!!
        )
      }
    }

    override fun toResponse(writer: ResponseWriter, value: TestQuery.R2) {
      writer.writeString(RESPONSE_FIELDS[0], value.name)
      writer.writeObject(RESPONSE_FIELDS[1]) { writer ->
        TestQuery_ResponseAdapter.FriendsConnection_ResponseAdapter.toResponse(writer, value.friendsConnection)
      }
    }
  }

  object Node1_ResponseAdapter : ResponseAdapter<TestQuery.Node1> {
    private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
      ResponseField.forString("name", "name", null, false, null)
    )

    override fun fromResponse(reader: ResponseReader, __typename: String?): TestQuery.Node1 {
      return reader.run {
        var name: String? = null
        while(true) {
          when (selectField(RESPONSE_FIELDS)) {
            0 -> name = readString(RESPONSE_FIELDS[0])
            else -> break
          }
        }
        TestQuery.Node1(
          name = name!!
        )
      }
    }

    override fun toResponse(writer: ResponseWriter, value: TestQuery.Node1) {
      writer.writeString(RESPONSE_FIELDS[0], value.name)
    }
  }

  object Edge1_ResponseAdapter : ResponseAdapter<TestQuery.Edge1> {
    private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
      ResponseField.forObject("node", "node", null, true, null)
    )

    override fun fromResponse(reader: ResponseReader, __typename: String?): TestQuery.Edge1 {
      return reader.run {
        var node: TestQuery.Node1? = null
        while(true) {
          when (selectField(RESPONSE_FIELDS)) {
            0 -> node = readObject<TestQuery.Node1>(RESPONSE_FIELDS[0]) { reader ->
              TestQuery_ResponseAdapter.Node1_ResponseAdapter.fromResponse(reader)
            }
            else -> break
          }
        }
        TestQuery.Edge1(
          node = node
        )
      }
    }

    override fun toResponse(writer: ResponseWriter, value: TestQuery.Edge1) {
      if(value.node == null) {
        writer.writeObject(RESPONSE_FIELDS[0], null)
      } else {
        writer.writeObject(RESPONSE_FIELDS[0]) { writer ->
          TestQuery_ResponseAdapter.Node1_ResponseAdapter.toResponse(writer, value.node)
        }
      }
    }
  }

  object FriendsConnection1_ResponseAdapter : ResponseAdapter<TestQuery.FriendsConnection1> {
    private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
      ResponseField.forInt("totalCount", "totalCount", null, true, null),
      ResponseField.forList("edges", "edges", null, true, null)
    )

    override fun fromResponse(reader: ResponseReader, __typename: String?):
        TestQuery.FriendsConnection1 {
      return reader.run {
        var totalCount: Int? = null
        var edges: List<TestQuery.Edge1?>? = null
        while(true) {
          when (selectField(RESPONSE_FIELDS)) {
            0 -> totalCount = readInt(RESPONSE_FIELDS[0])
            1 -> edges = readList<TestQuery.Edge1>(RESPONSE_FIELDS[1]) { reader ->
              reader.readObject<TestQuery.Edge1> { reader ->
                TestQuery_ResponseAdapter.Edge1_ResponseAdapter.fromResponse(reader)
              }
            }
            else -> break
          }
        }
        TestQuery.FriendsConnection1(
          totalCount = totalCount,
          edges = edges
        )
      }
    }

    override fun toResponse(writer: ResponseWriter, value: TestQuery.FriendsConnection1) {
      writer.writeInt(RESPONSE_FIELDS[0], value.totalCount)
      writer.writeList(RESPONSE_FIELDS[1], value.edges) { values, listItemWriter ->
        values?.forEach { value ->
          if(value == null) {
            listItemWriter.writeObject(null)
          } else {
            listItemWriter.writeObject { writer ->
              TestQuery_ResponseAdapter.Edge1_ResponseAdapter.toResponse(writer, value)
            }
          }
        }
      }
    }
  }

  object Luke_ResponseAdapter : ResponseAdapter<TestQuery.Luke> {
    private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
      ResponseField.forCustomType("id", "id", null, false, CustomType.ID, null),
      ResponseField.forString("name", "name", null, false, null),
      ResponseField.forObject("friendsConnection", "friendsConnection", null, false, null)
    )

    override fun fromResponse(reader: ResponseReader, __typename: String?): TestQuery.Luke {
      return reader.run {
        var id: String? = null
        var name: String? = null
        var friendsConnection: TestQuery.FriendsConnection1? = null
        while(true) {
          when (selectField(RESPONSE_FIELDS)) {
            0 -> id = readCustomType<String>(RESPONSE_FIELDS[0] as ResponseField.CustomTypeField)
            1 -> name = readString(RESPONSE_FIELDS[1])
            2 -> friendsConnection = readObject<TestQuery.FriendsConnection1>(RESPONSE_FIELDS[2]) { reader ->
              TestQuery_ResponseAdapter.FriendsConnection1_ResponseAdapter.fromResponse(reader)
            }
            else -> break
          }
        }
        TestQuery.Luke(
          id = id!!,
          name = name!!,
          friendsConnection = friendsConnection!!
        )
      }
    }

    override fun toResponse(writer: ResponseWriter, value: TestQuery.Luke) {
      writer.writeCustom(RESPONSE_FIELDS[0] as ResponseField.CustomTypeField, value.id)
      writer.writeString(RESPONSE_FIELDS[1], value.name)
      writer.writeObject(RESPONSE_FIELDS[2]) { writer ->
        TestQuery_ResponseAdapter.FriendsConnection1_ResponseAdapter.toResponse(writer, value.friendsConnection)
      }
    }
  }
}
