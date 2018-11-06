package com.example.fragment_friends_connection.fragment

import com.apollographql.apollo.api.GraphqlFragment
import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.ResponseFieldMarshaller
import com.apollographql.apollo.api.ResponseReader
import javax.annotation.Generated
import kotlin.Array
import kotlin.Int
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List

/**
 * @param name The name of the character
 * @param friendsConnection The friends of the character exposed as a connection with edges
 */
@Generated("Apollo GraphQL")
data class HeroDetails(
    val __typename: String,
    val name: String,
    val friendsConnection: FriendsConnection
) : GraphqlFragment {
    override fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
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

        val FRAGMENT_DEFINITION: String = """
                |fragment HeroDetails on Character {
                |  __typename
                |  name
                |  friendsConnection {
                |    __typename
                |    totalCount
                |    edges {
                |      __typename
                |      node {
                |        __typename
                |        name
                |      }
                |    }
                |  }
                |}
                """.trimMargin()

        val POSSIBLE_TYPES: Array<String> = arrayOf("Human", "Droid")

        operator fun invoke(reader: ResponseReader): HeroDetails {
            val __typename = reader.readString(RESPONSE_FIELDS[0])
            val name = reader.readString(RESPONSE_FIELDS[1])
            val friendsConnection = reader.readObject<FriendsConnection>(RESPONSE_FIELDS[2]) {
                FriendsConnection(it)
            }

            return HeroDetails(
                __typename = __typename,
                name = name,
                friendsConnection = friendsConnection
            )
        }
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
     * @param totalCount The total number of friends
     * @param edges The edges for each of the character's friends.
     */
    data class FriendsConnection(
        val __typename: String,
        val totalCount: Int?,
        val edges: List<Edge>?
    ) {
        fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
            it.writeString(RESPONSE_FIELDS[0], __typename)
            it.writeInt(RESPONSE_FIELDS[1], totalCount)
            it.writeList(RESPONSE_FIELDS[2], edges) { value, listItemWriter ->
                @Suppress("NAME_SHADOWING")
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
                    it.readObject<Edge> {
                        Edge(it)
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
}
