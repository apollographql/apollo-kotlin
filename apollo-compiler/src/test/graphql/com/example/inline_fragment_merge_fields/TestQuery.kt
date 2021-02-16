// AUTO-GENERATED FILE. DO NOT MODIFY.
//
// This class was automatically generated by Apollo GraphQL plugin from the GraphQL queries it found.
// It should not be modified by hand.
//
package com.example.inline_fragment_merge_fields

import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.api.ResponseAdapterCache
import com.apollographql.apollo3.api.ResponseField
import com.apollographql.apollo3.api.internal.QueryDocumentMinifier
import com.apollographql.apollo3.api.internal.ResponseAdapter
import com.example.inline_fragment_merge_fields.adapter.TestQuery_ResponseAdapter
import kotlin.Any
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List

@Suppress("NAME_SHADOWING", "UNUSED_ANONYMOUS_PARAMETER", "LocalVariableName",
    "RemoveExplicitTypeArguments", "NestedLambdaShadowedImplicitParameter", "PropertyName",
    "RemoveRedundantQualifierName")
class TestQuery : Query<TestQuery.Data> {
  override fun operationId(): String = OPERATION_ID

  override fun queryDocument(): String = QUERY_DOCUMENT

  override fun variables(): Operation.Variables = Operation.EMPTY_VARIABLES

  override fun name(): String = OPERATION_NAME

  override fun adapter(customScalarAdapters: ResponseAdapterCache): ResponseAdapter<Data> {
    val adapter = customScalarAdapters.getOperationAdapter(name()) {
      TestQuery_ResponseAdapter(customScalarAdapters)
    }
    return adapter
  }

  override fun responseFields(): List<ResponseField.FieldSet> = listOf(
    ResponseField.FieldSet(null, TestQuery_ResponseAdapter.RESPONSE_FIELDS)
  )
  /**
   * The query type, represents all of the entry points into our object graph
   */
  data class Data(
    val hero: Hero?
  ) : Operation.Data {
    /**
     * A character from the Star Wars universe
     */
    interface Hero {
      val __typename: String

      /**
       * The name of the character
       */
      val name: String

      /**
       * The friends of the character exposed as a connection with edges
       */
      val friendsConnection: FriendsConnection

      /**
       * A connection object for a character's friends
       */
      interface FriendsConnection {
        /**
         * The edges for each of the character's friends.
         */
        val edges: List<Edges?>?

        /**
         * An edge object for a character's friends
         */
        interface Edges {
          /**
           * The character represented by this friendship edge
           */
          val node: Node?

          /**
           * A character from the Star Wars universe
           */
          interface Node {
            /**
             * The name of the character
             */
            val name: String
          }
        }
      }

      data class CharacterHero(
        override val __typename: String,
        /**
         * The name of the character
         */
        override val name: String,
        /**
         * The friends of the character exposed as a connection with edges
         */
        override val friendsConnection: FriendsConnection,
        /**
         * Profile link
         */
        val profileLink: Any
      ) : Hero {
        /**
         * A connection object for a character's friends
         */
        data class FriendsConnection(
          /**
           * The edges for each of the character's friends.
           */
          override val edges: List<Edges?>?
        ) : Hero.FriendsConnection {
          /**
           * An edge object for a character's friends
           */
          data class Edges(
            /**
             * The character represented by this friendship edge
             */
            override val node: Node?
          ) : Hero.FriendsConnection.Edges {
            /**
             * A character from the Star Wars universe
             */
            data class Node(
              /**
               * The name of the character
               */
              override val name: String
            ) : Hero.FriendsConnection.Edges.Node
          }
        }
      }

      data class OtherHero(
        override val __typename: String,
        /**
         * The name of the character
         */
        override val name: String,
        /**
         * The friends of the character exposed as a connection with edges
         */
        override val friendsConnection: FriendsConnection
      ) : Hero {
        /**
         * A connection object for a character's friends
         */
        data class FriendsConnection(
          /**
           * The edges for each of the character's friends.
           */
          override val edges: List<Edges?>?
        ) : Hero.FriendsConnection {
          /**
           * An edge object for a character's friends
           */
          data class Edges(
            /**
             * The character represented by this friendship edge
             */
            override val node: Node?
          ) : Hero.FriendsConnection.Edges {
            /**
             * A character from the Star Wars universe
             */
            data class Node(
              /**
               * The name of the character
               */
              override val name: String
            ) : Hero.FriendsConnection.Edges.Node
          }
        }
      }

      companion object {
        fun Hero.asCharacterHero(): CharacterHero? = this as? CharacterHero
      }
    }
  }

  companion object {
    const val OPERATION_ID: String =
        "516a2d4b3fe97536486d2d559538aab9949d7c7e0de7081caec02439a0cb3969"

    val QUERY_DOCUMENT: String = QueryDocumentMinifier.minify(
          """
          |query TestQuery {
          |  hero {
          |    __typename
          |    name
          |    friendsConnection {
          |      edges {
          |        node {
          |          name
          |        }
          |      }
          |    }
          |    ... on Character {
          |      name
          |      profileLink
          |      friendsConnection {
          |        edges {
          |          node {
          |            name
          |          }
          |        }
          |      }
          |    }
          |  }
          |}
          """.trimMargin()
        )

    val OPERATION_NAME: String = "TestQuery"
  }
}
