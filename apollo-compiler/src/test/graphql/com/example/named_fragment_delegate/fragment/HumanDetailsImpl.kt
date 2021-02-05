// AUTO-GENERATED FILE. DO NOT MODIFY.
//
// This class was automatically generated by Apollo GraphQL plugin from the GraphQL queries it found.
// It should not be modified by hand.
//
package com.example.named_fragment_delegate.fragment

import com.apollographql.apollo.api.Fragment
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.internal.ResponseAdapter
import com.example.named_fragment_delegate.fragment.adapter.HumanDetailsImpl_ResponseAdapter
import kotlin.Any
import kotlin.String
import kotlin.collections.List

class HumanDetailsImpl : Fragment<HumanDetailsImpl.Data> {
  override fun adapter(): ResponseAdapter<Data> {
    return HumanDetailsImpl_ResponseAdapter
  }

  override fun responseFields(): List<ResponseField.FieldSet> = listOf(
    ResponseField.FieldSet(null, HumanDetailsImpl_ResponseAdapter.RESPONSE_FIELDS)
  )
  override fun variables(): Operation.Variables = Operation.EMPTY_VARIABLES

  /**
   * A humanoid creature from the Star Wars universe
   */
  data class Data(
    override val __typename: String = "Human",
    /**
     * What this human calls themselves
     */
    override val name: String,
    /**
     * Profile link
     */
    override val profileLink: Any,
    /**
     * The friends of the human exposed as a connection with edges
     */
    override val friendsConnection: FriendsConnection
  ) : HumanDetails, Fragment.Data {
    /**
     * A connection object for a character's friends
     */
    data class FriendsConnection(
      /**
       * The edges for each of the character's friends.
       */
      override val edges: List<Edges?>?
    ) : HumanDetails.FriendsConnection {
      /**
       * An edge object for a character's friends
       */
      data class Edges(
        /**
         * The character represented by this friendship edge
         */
        override val node: Node?
      ) : HumanDetails.FriendsConnection.Edges {
        /**
         * A character from the Star Wars universe
         */
        data class Node(
          /**
           * The name of the character
           */
          override val name: String
        ) : HumanDetails.FriendsConnection.Edges.Node
      }
    }
  }
}
