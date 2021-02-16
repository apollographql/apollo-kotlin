// AUTO-GENERATED FILE. DO NOT MODIFY.
//
// This class was automatically generated by Apollo GraphQL plugin from the GraphQL queries it found.
// It should not be modified by hand.
//
package com.example.fragment_with_inline_fragment.fragment

import com.apollographql.apollo3.api.Fragment
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.ResponseAdapterCache
import com.apollographql.apollo3.api.ResponseField
import com.apollographql.apollo3.api.internal.ResponseAdapter
import com.example.fragment_with_inline_fragment.fragment.adapter.HeroDetailsImpl_ResponseAdapter
import kotlin.Int
import kotlin.String
import kotlin.collections.List

class HeroDetailsImpl : Fragment<HeroDetailsImpl.Data> {
  override fun adapter(customScalarAdapters: ResponseAdapterCache): ResponseAdapter<Data> {
    val adapter = customScalarAdapters.getFragmentAdapter("HeroDetailsImpl") {
      HeroDetailsImpl_ResponseAdapter(customScalarAdapters)
    }
    return adapter
  }

  override fun responseFields(): List<ResponseField.FieldSet> = listOf(
    ResponseField.FieldSet("Droid", HeroDetailsImpl_ResponseAdapter.DroidData.RESPONSE_FIELDS),
    ResponseField.FieldSet("Human", HeroDetailsImpl_ResponseAdapter.HumanData.RESPONSE_FIELDS),
    ResponseField.FieldSet(null, HeroDetailsImpl_ResponseAdapter.OtherData.RESPONSE_FIELDS),
  )
  override fun variables(): Operation.Variables = Operation.EMPTY_VARIABLES

  /**
   * A character from the Star Wars universe
   */
  interface Data : HeroDetails, Fragment.Data {
    data class DroidData(
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
       * This droid's primary function
       */
      override val primaryFunction: String?
    ) : HeroDetails, HeroDetails.Droid, HeroDetails.Droid.Droid, DroidDetails, Data {
      /**
       * A connection object for a character's friends
       */
      data class FriendsConnection(
        /**
         * The total number of friends
         */
        override val totalCount: Int?,
        /**
         * The edges for each of the character's friends.
         */
        override val edges: List<Edges?>?
      ) : HeroDetails.FriendsConnection, HeroDetails.Droid.FriendsConnection,
          HeroDetails.Droid.Droid.FriendsConnection {
        /**
         * An edge object for a character's friends
         */
        data class Edges(
          /**
           * The character represented by this friendship edge
           */
          override val node: Node?
        ) : HeroDetails.FriendsConnection.Edges, HeroDetails.Droid.FriendsConnection.Edges,
            HeroDetails.Droid.Droid.FriendsConnection.Edges {
          /**
           * A character from the Star Wars universe
           */
          data class Node(
            /**
             * The name of the character
             */
            override val name: String
          ) : HeroDetails.FriendsConnection.Edges.Node,
              HeroDetails.Droid.FriendsConnection.Edges.Node,
              HeroDetails.Droid.Droid.FriendsConnection.Edges.Node
        }
      }
    }

    data class HumanData(
      override val __typename: String,
      /**
       * The name of the character
       */
      override val name: String,
      /**
       * The friends of the character exposed as a connection with edges
       */
      override val friendsConnection: FriendsConnection
    ) : HeroDetails, HeroDetails.Human, HumanDetails, Data {
      /**
       * A connection object for a character's friends
       */
      data class FriendsConnection(
        /**
         * The total number of friends
         */
        override val totalCount: Int?,
        /**
         * The edges for each of the character's friends.
         */
        override val edges: List<Edges?>?
      ) : HeroDetails.FriendsConnection, HeroDetails.Human.FriendsConnection {
        /**
         * An edge object for a character's friends
         */
        data class Edges(
          /**
           * The character represented by this friendship edge
           */
          override val node: Node?
        ) : HeroDetails.FriendsConnection.Edges, HeroDetails.Human.FriendsConnection.Edges {
          /**
           * A character from the Star Wars universe
           */
          data class Node(
            /**
             * The name of the character
             */
            override val name: String
          ) : HeroDetails.FriendsConnection.Edges.Node,
              HeroDetails.Human.FriendsConnection.Edges.Node
        }
      }
    }

    data class OtherData(
      override val __typename: String,
      /**
       * The name of the character
       */
      override val name: String,
      /**
       * The friends of the character exposed as a connection with edges
       */
      override val friendsConnection: FriendsConnection
    ) : HeroDetails, Data {
      /**
       * A connection object for a character's friends
       */
      data class FriendsConnection(
        /**
         * The total number of friends
         */
        override val totalCount: Int?,
        /**
         * The edges for each of the character's friends.
         */
        override val edges: List<Edges?>?
      ) : HeroDetails.FriendsConnection {
        /**
         * An edge object for a character's friends
         */
        data class Edges(
          /**
           * The character represented by this friendship edge
           */
          override val node: Node?
        ) : HeroDetails.FriendsConnection.Edges {
          /**
           * A character from the Star Wars universe
           */
          data class Node(
            /**
             * The name of the character
             */
            override val name: String
          ) : HeroDetails.FriendsConnection.Edges.Node
        }
      }
    }
  }
}
