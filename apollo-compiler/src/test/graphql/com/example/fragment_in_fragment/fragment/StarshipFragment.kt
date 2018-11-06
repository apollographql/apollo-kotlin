package com.example.fragment_in_fragment.fragment

import com.apollographql.apollo.api.GraphqlFragment
import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.ResponseFieldMarshaller
import com.apollographql.apollo.api.ResponseReader
import com.example.fragment_in_fragment.type.CustomType
import javax.annotation.Generated
import kotlin.Array
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List

/**
 * @param id The ID of an object
 * @param name The name of this starship. The common name, such as "Death Star".
 */
@Generated("Apollo GraphQL")
data class StarshipFragment(
    val __typename: String,
    val id: String,
    val name: String?,
    val pilotConnection: PilotConnection?
) : GraphqlFragment {
    override fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
        it.writeString(RESPONSE_FIELDS[0], __typename)
        it.writeCustom(RESPONSE_FIELDS[1] as ResponseField.CustomTypeField, id)
        it.writeString(RESPONSE_FIELDS[2], name)
        it.writeObject(RESPONSE_FIELDS[3], pilotConnection?.marshaller())
    }

    companion object {
        private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
                ResponseField.forString("__typename", "__typename", null, false, null),
                ResponseField.forCustomType("id", "id", null, false, CustomType.ID, null),
                ResponseField.forString("name", "name", null, true, null),
                ResponseField.forObject("pilotConnection", "pilotConnection", null, true, null)
                )

        val FRAGMENT_DEFINITION: String = """
                |fragment starshipFragment on Starship {
                |  __typename
                |  id
                |  name
                |  pilotConnection {
                |    __typename
                |    edges {
                |      __typename
                |      node {
                |        __typename
                |        ...pilotFragment
                |      }
                |    }
                |  }
                |}
                """.trimMargin()

        val POSSIBLE_TYPES: Array<String> = arrayOf("Starship")

        operator fun invoke(reader: ResponseReader): StarshipFragment {
            val __typename = reader.readString(RESPONSE_FIELDS[0])
            val id = reader.readCustomType<String>(RESPONSE_FIELDS[1] as ResponseField.CustomTypeField)
            val name = reader.readString(RESPONSE_FIELDS[2])
            val pilotConnection = reader.readObject<PilotConnection>(RESPONSE_FIELDS[3]) {
                PilotConnection(it)
            }

            return StarshipFragment(
                __typename = __typename,
                id = id,
                name = name,
                pilotConnection = pilotConnection
            )
        }
    }

    data class Node(val __typename: String, val fragments: Fragments) {
        fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
            it.writeString(RESPONSE_FIELDS[0], __typename)
            fragments.marshaller().marshal(it)
        }

        companion object {
            private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
                    ResponseField.forString("__typename", "__typename", null, false, null),
                    ResponseField.forString("__typename", "__typename", null, false, null)
                    )

            operator fun invoke(reader: ResponseReader): Node {
                val __typename = reader.readString(RESPONSE_FIELDS[0])
                val fragments = reader.readConditional(RESPONSE_FIELDS[1]) { conditionalType, reader ->
                    val pilotFragment = if (PilotFragment.POSSIBLE_TYPES.contains(conditionalType)) PilotFragment(reader) else null
                    Fragments(
                        pilotFragment = pilotFragment!!
                    )
                }

                return Node(
                    __typename = __typename,
                    fragments = fragments
                )
            }
        }

        data class Fragments(val pilotFragment: PilotFragment) {
            fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
                pilotFragment.marshaller().marshal(it)
            }
        }
    }

    /**
     * @param node The item at the end of the edge
     */
    data class Edge(
        val __typename: String,
        val node: Node?
    ) {
        fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
            it.writeString(RESPONSE_FIELDS[0], __typename)
            it.writeObject(RESPONSE_FIELDS[1], node?.marshaller())
        }

        companion object {
            private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
                    ResponseField.forString("__typename", "__typename", null, false, null),
                    ResponseField.forObject("node", "node", null, true, null)
                    )

            operator fun invoke(reader: ResponseReader): Edge {
                val __typename = reader.readString(RESPONSE_FIELDS[0])
                val node = reader.readObject<Node>(RESPONSE_FIELDS[1]) {
                    Node(it)
                }

                return Edge(
                    __typename = __typename,
                    node = node
                )
            }
        }
    }

    /**
     * @param edges A list of edges.
     */
    data class PilotConnection(
        val __typename: String,
        val edges: List<Edge>?
    ) {
        fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
            it.writeString(RESPONSE_FIELDS[0], __typename)
            it.writeList(RESPONSE_FIELDS[1], edges) { value, listItemWriter ->
                @Suppress("NAME_SHADOWING")
                value?.forEach { value ->
                    listItemWriter.writeObject(value?.marshaller())
                }
            }
        }

        companion object {
            private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
                    ResponseField.forString("__typename", "__typename", null, false, null),
                    ResponseField.forList("edges", "edges", null, true, null)
                    )

            operator fun invoke(reader: ResponseReader): PilotConnection {
                val __typename = reader.readString(RESPONSE_FIELDS[0])
                val edges = reader.readList<Edge>(RESPONSE_FIELDS[1]) {
                    it.readObject<Edge> {
                        Edge(it)
                    }

                }
                return PilotConnection(
                    __typename = __typename,
                    edges = edges
                )
            }
        }
    }
}
