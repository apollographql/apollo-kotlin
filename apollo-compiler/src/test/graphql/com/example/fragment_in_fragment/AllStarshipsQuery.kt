package com.example.fragment_in_fragment

import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.OperationName
import com.apollographql.apollo.api.Query
import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.ResponseFieldMapper
import com.apollographql.apollo.api.ResponseFieldMarshaller
import com.apollographql.apollo.api.ResponseReader
import com.example.fragment_in_fragment.fragment.StarshipFragment
import javax.annotation.Generated
import kotlin.Array
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List

@Generated("Apollo GraphQL")
class AllStarshipsQuery : Query<AllStarshipsQuery.Data, AllStarshipsQuery.Data, Operation.Variables> {
    override fun operationId(): String = OPERATION_ID
    override fun queryDocument(): String = QUERY_DOCUMENT
    override fun wrapData(data: AllStarshipsQuery.Data): AllStarshipsQuery.Data = data
    override fun variables(): Operation.Variables = Operation.EMPTY_VARIABLES
    override fun name(): OperationName = OPERATION_NAME
    override fun responseFieldMapper(): ResponseFieldMapper<AllStarshipsQuery.Data> = ResponseFieldMapper {
        AllStarshipsQuery.Data(it)
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
                    val starshipFragment = if (StarshipFragment.POSSIBLE_TYPES.contains(conditionalType)) StarshipFragment(reader) else null
                    Fragments(
                        starshipFragment = starshipFragment!!
                    )
                }

                return Node(
                    __typename = __typename,
                    fragments = fragments
                )
            }
        }

        data class Fragments(val starshipFragment: StarshipFragment) {
            fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
                starshipFragment.marshaller().marshal(it)
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
    data class AllStarship(
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

            operator fun invoke(reader: ResponseReader): AllStarship {
                val __typename = reader.readString(RESPONSE_FIELDS[0])
                val edges = reader.readList<Edge>(RESPONSE_FIELDS[1]) {
                    it.readObject<Edge> {
                        Edge(it)
                    }

                }
                return AllStarship(
                    __typename = __typename,
                    edges = edges
                )
            }
        }
    }

    data class Data(val allStarships: AllStarship?) : Operation.Data {
        override fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
            it.writeObject(RESPONSE_FIELDS[0], allStarships?.marshaller())
        }

        companion object {
            private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
                    ResponseField.forObject("allStarships", "allStarships", mapOf<String, Any>(
                        "first" to "7"), true, null)
                    )

            operator fun invoke(reader: ResponseReader): Data {
                val allStarships = reader.readObject<AllStarship>(RESPONSE_FIELDS[0]) {
                    AllStarship(it)
                }

                return Data(
                    allStarships = allStarships
                )
            }
        }
    }

    companion object {
        val OPERATION_DEFINITION: String = """
                |query AllStarships {
                |  allStarships(first: 7) {
                |    __typename
                |    edges {
                |      __typename
                |      node {
                |        __typename
                |        ...starshipFragment
                |      }
                |    }
                |  }
                |}
                """.trimMargin()

        const val OPERATION_ID: String =
                "f3b63150118cfccd52140c4ca6aec578235d7ea99c5b905f14138c49f7f5fc7d"

        val QUERY_DOCUMENT: String = """
                |query AllStarships {
                |  allStarships(first: 7) {
                |    __typename
                |    edges {
                |      __typename
                |      node {
                |        __typename
                |        ...starshipFragment
                |      }
                |    }
                |  }
                |}
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
                |fragment pilotFragment on Person {
                |  __typename
                |  name
                |  homeworld {
                |    __typename
                |    name
                |  }
                |}
                """.trimMargin()

        val OPERATION_NAME: OperationName = OperationName { "AllStarshipsQuery" }
    }
}
