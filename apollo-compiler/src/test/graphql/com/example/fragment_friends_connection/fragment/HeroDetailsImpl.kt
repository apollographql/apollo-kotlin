// AUTO-GENERATED FILE. DO NOT MODIFY.
//
// This class was automatically generated by Apollo GraphQL plugin from the GraphQL queries it found.
// It should not be modified by hand.
//
package com.example.fragment_friends_connection.fragment

import com.apollographql.apollo.api.Fragment
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.internal.ResponseAdapter
import com.example.fragment_friends_connection.fragment.adapter.HeroDetailsImpl_ResponseAdapter
import kotlin.Int
import kotlin.String
import kotlin.collections.List

class HeroDetailsImpl : Fragment<HeroDetailsImpl.Data> {
  override fun adapter(): ResponseAdapter<Data> {
    return HeroDetailsImpl_ResponseAdapter
  }

  override fun responseFields(): List<ResponseField.FieldSet> = listOf(
    ResponseField.FieldSet(null, HeroDetailsImpl_ResponseAdapter.RESPONSE_FIELDS)
  )
  override fun variables(): Operation.Variables = Operation.EMPTY_VARIABLES

  /**
   * A character from the Star Wars universe
   */
  data class Data(
    override val __typename: String = "Character",
    /**
     * The name of the character
     */
    override val name: String,
    /**
     * The friends of the character exposed as a connection with edges
     */
    override val friendsConnection: FriendsConnection
  ) : HeroDetails, Fragment.Data {
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
