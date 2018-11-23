package com.example.hero_details_semantic_naming

import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.OperationName
import com.apollographql.apollo.api.Query
import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.ResponseFieldMapper
import com.apollographql.apollo.api.ResponseFieldMarshaller
import com.apollographql.apollo.api.ResponseReader
import javax.annotation.Generated
import kotlin.Array
import kotlin.Int
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List

@Generated("Apollo GraphQL")
@Suppress("NAME_SHADOWING", "LocalVariableName")
class HeroDetailsQuery : Query<HeroDetailsQuery.Data, HeroDetailsQuery.Data, Operation.Variables> {
    override fun operationId(): String = OPERATION_ID
    override fun queryDocument(): String = QUERY_DOCUMENT
    override fun wrapData(data: HeroDetailsQuery.Data): HeroDetailsQuery.Data = data
    override fun variables(): Operation.Variables = Operation.EMPTY_VARIABLES
    override fun name(): OperationName = OPERATION_NAME
    override fun responseFieldMapper(): ResponseFieldMapper<HeroDetailsQuery.Data> = ResponseFieldMapper {
        HeroDetailsQuery.Data(it)
    }

    /**
     * @param name The name of the character
     */
    data class Node(
        val __typename: String,
        val name: String
    ) {
        fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
            it.writeString(RESPONSE_FIELDS[0], __typename)
            it.writeString(RESPONSE_FIELDS[1], name)
        }

        companion object {
            private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
                    ResponseField.forString("__typename", "__typename", null, false, null),
                    ResponseField.forString("name", "name", null, false, null)
                    )

            operator fun invoke(reader: ResponseReader): Node {
                val __typename = reader.readString(RESPONSE_FIELDS[0])
                val name = reader.readString(RESPONSE_FIELDS[1])
                return Node(
                    __typename = __typename,
                    name = name
                )
            }
        }
    }

    /**
     * @param node The character represented by this friendship edge
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
                val node = reader.readObject<Node>(RESPONSE_FIELDS[1]) { reader ->
                    Node(reader)
                }

                return Edge(
                    __typename = __typename,
                    node = node
                )
            }
        }
    }

    /**
     * @param totalCount The total number of friends
     * @param edges The edges for each of the character's friends.
     */
    data class FriendsConnection(
        val __typename: String,
        val totalCount: Int?,
        val edges: List<Edge?>?
    ) {
        fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
            it.writeString(RESPONSE_FIELDS[0], __typename)
            it.writeInt(RESPONSE_FIELDS[1], totalCount)
            it.writeList(RESPONSE_FIELDS[2], edges) { value, listItemWriter ->
                value?.forEach { value ->
                    listItemWriter.writeObject(value?.marshaller())
                }
            }
        }

        companion object {
            private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
                    ResponseField.forString("__typename", "__typename", null, false, null),
                    ResponseField.forInt("totalCount", "totalCount", null, true, null),
                    ResponseField.forList("edges", "edges", null, true, null)
                    )

            operator fun invoke(reader: ResponseReader): FriendsConnection {
                val __typename = reader.readString(RESPONSE_FIELDS[0])
                val totalCount = reader.readInt(RESPONSE_FIELDS[1])
                val edges = reader.readList<Edge>(RESPONSE_FIELDS[2]) {
                    it.readObject<Edge> { reader ->
                        Edge(reader)
                    }

                }
                return FriendsConnection(
                    __typename = __typename,
                    totalCount = totalCount,
                    edges = edges
                )
            }
        }
    }

    /**
     * @param name The name of the character
     * @param friendsConnection The friends of the character exposed as a connection with edges
     */
    data class Hero(
        val __typename: String,
        val name: String,
        val friendsConnection: FriendsConnection
    ) {
        fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
            it.writeString(RESPONSE_FIELDS[0], __typename)
            it.writeString(RESPONSE_FIELDS[1], name)
            it.writeObject(RESPONSE_FIELDS[2], friendsConnection.marshaller())
        }

        companion object {
            private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
                    ResponseField.forString("__typename", "__typename", null, false, null),
                    ResponseField.forString("name", "name", null, false, null),
                    ResponseField.forObject("friendsConnection", "friendsConnection", null, false, null)
                    )

            operator fun invoke(reader: ResponseReader): Hero {
                val __typename = reader.readString(RESPONSE_FIELDS[0])
                val name = reader.readString(RESPONSE_FIELDS[1])
                val friendsConnection = reader.readObject<FriendsConnection>(RESPONSE_FIELDS[2]) { reader ->
                    FriendsConnection(reader)
                }

                return Hero(
                    __typename = __typename,
                    name = name,
                    friendsConnection = friendsConnection
                )
            }
        }
    }

    data class Data(val hero: Hero?) : Operation.Data {
        override fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
            it.writeObject(RESPONSE_FIELDS[0], hero?.marshaller())
        }

        companion object {
            private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
                    ResponseField.forObject("hero", "hero", null, true, null)
                    )

            operator fun invoke(reader: ResponseReader): Data {
                val hero = reader.readObject<Hero>(RESPONSE_FIELDS[0]) { reader ->
                    Hero(reader)
                }

                return Data(
                    hero = hero
                )
            }
        }
    }

    companion object {
        val OPERATION_DEFINITION: String = """
                |query HeroDetails {
                |  hero {
                |    __typename
                |    name
                |    friendsConnection {
                |      __typename
                |      totalCount
                |      edges {
                |        __typename
                |        node {
                |          __typename
                |          name
                |        }
                |      }
                |    }
                |  }
                |}
                """.trimMargin()

        const val OPERATION_ID: String =
                "04c4c609078258b0ad954a88e05fb068e53815a35051bb61cde8cc14107583bc"

        val QUERY_DOCUMENT: String = """
                |query HeroDetails {
                |  hero {
                |    __typename
                |    name
                |    friendsConnection {
                |      __typename
                |      totalCount
                |      edges {
                |        __typename
                |        node {
                |          __typename
                |          name
                |        }
                |      }
                |    }
                |  }
                |}
                """.trimMargin()

        val OPERATION_NAME: OperationName = OperationName { "HeroDetailsQuery" }
    }
}
