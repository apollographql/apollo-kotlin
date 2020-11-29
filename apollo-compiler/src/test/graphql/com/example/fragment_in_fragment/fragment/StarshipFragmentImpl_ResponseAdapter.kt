// AUTO-GENERATED FILE. DO NOT MODIFY.
//
// This class was automatically generated by Apollo GraphQL plugin from the GraphQL queries it found.
// It should not be modified by hand.
//
package com.example.fragment_in_fragment.fragment

import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.internal.ResponseAdapter
import com.apollographql.apollo.api.internal.ResponseReader
import com.apollographql.apollo.api.internal.ResponseWriter
import kotlin.Array
import kotlin.String
import kotlin.collections.List

object StarshipFragmentImpl_ResponseAdapter : ResponseAdapter<StarshipFragmentImpl> {
  private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
    ResponseField.forString("__typename", "__typename", null, false, null),
    ResponseField.forString("id", "id", null, false, null),
    ResponseField.forString("name", "name", null, true, null),
    ResponseField.forObject("pilotConnection", "pilotConnection", null, true, null)
  )

  override fun fromResponse(reader: ResponseReader, __typename: String?): StarshipFragmentImpl {
    return reader.run {
      var __typename: String? = __typename
      var id: String? = null
      var name: String? = null
      var pilotConnection: StarshipFragmentImpl.PilotConnection? = null
      while(true) {
        when (selectField(RESPONSE_FIELDS)) {
          0 -> __typename = readString(RESPONSE_FIELDS[0])
          1 -> id = readString(RESPONSE_FIELDS[1])
          2 -> name = readString(RESPONSE_FIELDS[2])
          3 -> pilotConnection = readObject<StarshipFragmentImpl.PilotConnection>(RESPONSE_FIELDS[3]) { reader ->
            PilotConnection.fromResponse(reader)
          }
          else -> break
        }
      }
      StarshipFragmentImpl(
        __typename = __typename!!,
        id = id!!,
        name = name,
        pilotConnection = pilotConnection
      )
    }
  }

  override fun toResponse(writer: ResponseWriter, value: StarshipFragmentImpl) {
    writer.writeString(RESPONSE_FIELDS[0], value.__typename)
    writer.writeString(RESPONSE_FIELDS[1], value.id)
    writer.writeString(RESPONSE_FIELDS[2], value.name)
    if(value.pilotConnection == null) {
      writer.writeObject(RESPONSE_FIELDS[3], null)
    } else {
      writer.writeObject(RESPONSE_FIELDS[3]) { writer ->
        PilotConnection.toResponse(writer, value.pilotConnection)
      }
    }
  }

  object PilotConnection : ResponseAdapter<StarshipFragmentImpl.PilotConnection> {
    private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
      ResponseField.forList("edges", "edges", null, true, null)
    )

    override fun fromResponse(reader: ResponseReader, __typename: String?):
        StarshipFragmentImpl.PilotConnection {
      return reader.run {
        var edges: List<StarshipFragmentImpl.PilotConnection.Edge?>? = null
        while(true) {
          when (selectField(RESPONSE_FIELDS)) {
            0 -> edges = readList<StarshipFragmentImpl.PilotConnection.Edge>(RESPONSE_FIELDS[0]) { reader ->
              reader.readObject<StarshipFragmentImpl.PilotConnection.Edge> { reader ->
                Edge.fromResponse(reader)
              }
            }
            else -> break
          }
        }
        StarshipFragmentImpl.PilotConnection(
          edges = edges
        )
      }
    }

    override fun toResponse(writer: ResponseWriter, value: StarshipFragmentImpl.PilotConnection) {
      writer.writeList(RESPONSE_FIELDS[0], value.edges) { values, listItemWriter ->
        values?.forEach { value ->
          if(value == null) {
            listItemWriter.writeObject(null)
          } else {
            listItemWriter.writeObject { writer ->
              Edge.toResponse(writer, value)
            }
          }
        }
      }
    }

    object Edge : ResponseAdapter<StarshipFragmentImpl.PilotConnection.Edge> {
      private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
        ResponseField.forObject("node", "node", null, true, null)
      )

      override fun fromResponse(reader: ResponseReader, __typename: String?):
          StarshipFragmentImpl.PilotConnection.Edge {
        return reader.run {
          var node: StarshipFragmentImpl.PilotConnection.Edge.Node? = null
          while(true) {
            when (selectField(RESPONSE_FIELDS)) {
              0 -> node = readObject<StarshipFragmentImpl.PilotConnection.Edge.Node>(RESPONSE_FIELDS[0]) { reader ->
                Node.fromResponse(reader)
              }
              else -> break
            }
          }
          StarshipFragmentImpl.PilotConnection.Edge(
            node = node
          )
        }
      }

      override fun toResponse(writer: ResponseWriter,
          value: StarshipFragmentImpl.PilotConnection.Edge) {
        if(value.node == null) {
          writer.writeObject(RESPONSE_FIELDS[0], null)
        } else {
          writer.writeObject(RESPONSE_FIELDS[0]) { writer ->
            Node.toResponse(writer, value.node)
          }
        }
      }

      object Node : ResponseAdapter<StarshipFragmentImpl.PilotConnection.Edge.Node> {
        private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
          ResponseField.forString("__typename", "__typename", null, false, null)
        )

        override fun fromResponse(reader: ResponseReader, __typename: String?):
            StarshipFragmentImpl.PilotConnection.Edge.Node {
          val typename = __typename ?: reader.readString(RESPONSE_FIELDS[0])
          return when(typename) {
            "Person" -> PersonNode.fromResponse(reader, typename)
            else -> OtherNode.fromResponse(reader, typename)
          }
        }

        override fun toResponse(writer: ResponseWriter,
            value: StarshipFragmentImpl.PilotConnection.Edge.Node) {
          when(value) {
            is StarshipFragmentImpl.PilotConnection.Edge.Node.PersonNode -> PersonNode.toResponse(writer, value)
            is StarshipFragmentImpl.PilotConnection.Edge.Node.OtherNode -> OtherNode.toResponse(writer, value)
          }
        }

        object PersonNode :
            ResponseAdapter<StarshipFragmentImpl.PilotConnection.Edge.Node.PersonNode> {
          private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
            ResponseField.forString("__typename", "__typename", null, false, null),
            ResponseField.forString("name", "name", null, true, null),
            ResponseField.forObject("homeworld", "homeworld", null, true, null)
          )

          override fun fromResponse(reader: ResponseReader, __typename: String?):
              StarshipFragmentImpl.PilotConnection.Edge.Node.PersonNode {
            return reader.run {
              var __typename: String? = __typename
              var name: String? = null
              var homeworld: StarshipFragmentImpl.PilotConnection.Edge.Node.PersonNode.Homeworld? = null
              while(true) {
                when (selectField(RESPONSE_FIELDS)) {
                  0 -> __typename = readString(RESPONSE_FIELDS[0])
                  1 -> name = readString(RESPONSE_FIELDS[1])
                  2 -> homeworld = readObject<StarshipFragmentImpl.PilotConnection.Edge.Node.PersonNode.Homeworld>(RESPONSE_FIELDS[2]) { reader ->
                    Homeworld.fromResponse(reader)
                  }
                  else -> break
                }
              }
              StarshipFragmentImpl.PilotConnection.Edge.Node.PersonNode(
                __typename = __typename!!,
                name = name,
                homeworld = homeworld
              )
            }
          }

          override fun toResponse(writer: ResponseWriter,
              value: StarshipFragmentImpl.PilotConnection.Edge.Node.PersonNode) {
            writer.writeString(RESPONSE_FIELDS[0], value.__typename)
            writer.writeString(RESPONSE_FIELDS[1], value.name)
            if(value.homeworld == null) {
              writer.writeObject(RESPONSE_FIELDS[2], null)
            } else {
              writer.writeObject(RESPONSE_FIELDS[2]) { writer ->
                Homeworld.toResponse(writer, value.homeworld)
              }
            }
          }

          object Homeworld :
              ResponseAdapter<StarshipFragmentImpl.PilotConnection.Edge.Node.PersonNode.Homeworld> {
            private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
              ResponseField.forString("__typename", "__typename", null, false, null)
            )

            override fun fromResponse(reader: ResponseReader, __typename: String?):
                StarshipFragmentImpl.PilotConnection.Edge.Node.PersonNode.Homeworld {
              val typename = __typename ?: reader.readString(RESPONSE_FIELDS[0])
              return when(typename) {
                "Planet" -> PlanetHomeworld.fromResponse(reader, typename)
                else -> OtherHomeworld.fromResponse(reader, typename)
              }
            }

            override fun toResponse(writer: ResponseWriter,
                value: StarshipFragmentImpl.PilotConnection.Edge.Node.PersonNode.Homeworld) {
              when(value) {
                is StarshipFragmentImpl.PilotConnection.Edge.Node.PersonNode.Homeworld.PlanetHomeworld -> PlanetHomeworld.toResponse(writer, value)
                is StarshipFragmentImpl.PilotConnection.Edge.Node.PersonNode.Homeworld.OtherHomeworld -> OtherHomeworld.toResponse(writer, value)
              }
            }

            object PlanetHomeworld :
                ResponseAdapter<StarshipFragmentImpl.PilotConnection.Edge.Node.PersonNode.Homeworld.PlanetHomeworld>
                {
              private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
                ResponseField.forString("__typename", "__typename", null, false, null),
                ResponseField.forString("name", "name", null, true, null)
              )

              override fun fromResponse(reader: ResponseReader, __typename: String?):
                  StarshipFragmentImpl.PilotConnection.Edge.Node.PersonNode.Homeworld.PlanetHomeworld {
                return reader.run {
                  var __typename: String? = __typename
                  var name: String? = null
                  while(true) {
                    when (selectField(RESPONSE_FIELDS)) {
                      0 -> __typename = readString(RESPONSE_FIELDS[0])
                      1 -> name = readString(RESPONSE_FIELDS[1])
                      else -> break
                    }
                  }
                  StarshipFragmentImpl.PilotConnection.Edge.Node.PersonNode.Homeworld.PlanetHomeworld(
                    __typename = __typename!!,
                    name = name
                  )
                }
              }

              override fun toResponse(writer: ResponseWriter,
                  value: StarshipFragmentImpl.PilotConnection.Edge.Node.PersonNode.Homeworld.PlanetHomeworld) {
                writer.writeString(RESPONSE_FIELDS[0], value.__typename)
                writer.writeString(RESPONSE_FIELDS[1], value.name)
              }
            }

            object OtherHomeworld :
                ResponseAdapter<StarshipFragmentImpl.PilotConnection.Edge.Node.PersonNode.Homeworld.OtherHomeworld>
                {
              private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
                ResponseField.forString("__typename", "__typename", null, false, null)
              )

              override fun fromResponse(reader: ResponseReader, __typename: String?):
                  StarshipFragmentImpl.PilotConnection.Edge.Node.PersonNode.Homeworld.OtherHomeworld {
                return reader.run {
                  var __typename: String? = __typename
                  while(true) {
                    when (selectField(RESPONSE_FIELDS)) {
                      0 -> __typename = readString(RESPONSE_FIELDS[0])
                      else -> break
                    }
                  }
                  StarshipFragmentImpl.PilotConnection.Edge.Node.PersonNode.Homeworld.OtherHomeworld(
                    __typename = __typename!!
                  )
                }
              }

              override fun toResponse(writer: ResponseWriter,
                  value: StarshipFragmentImpl.PilotConnection.Edge.Node.PersonNode.Homeworld.OtherHomeworld) {
                writer.writeString(RESPONSE_FIELDS[0], value.__typename)
              }
            }
          }
        }

        object OtherNode : ResponseAdapter<StarshipFragmentImpl.PilotConnection.Edge.Node.OtherNode>
            {
          private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
            ResponseField.forString("__typename", "__typename", null, false, null)
          )

          override fun fromResponse(reader: ResponseReader, __typename: String?):
              StarshipFragmentImpl.PilotConnection.Edge.Node.OtherNode {
            return reader.run {
              var __typename: String? = __typename
              while(true) {
                when (selectField(RESPONSE_FIELDS)) {
                  0 -> __typename = readString(RESPONSE_FIELDS[0])
                  else -> break
                }
              }
              StarshipFragmentImpl.PilotConnection.Edge.Node.OtherNode(
                __typename = __typename!!
              )
            }
          }

          override fun toResponse(writer: ResponseWriter,
              value: StarshipFragmentImpl.PilotConnection.Edge.Node.OtherNode) {
            writer.writeString(RESPONSE_FIELDS[0], value.__typename)
          }
        }
      }
    }
  }
}
