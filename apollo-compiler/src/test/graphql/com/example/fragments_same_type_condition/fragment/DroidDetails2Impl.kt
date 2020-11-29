// AUTO-GENERATED FILE. DO NOT MODIFY.
//
// This class was automatically generated by Apollo GraphQL plugin from the GraphQL queries it found.
// It should not be modified by hand.
//
package com.example.fragments_same_type_condition.fragment

import com.apollographql.apollo.api.GraphqlFragment
import com.apollographql.apollo.api.internal.ResponseFieldMarshaller
import kotlin.String

/**
 * An autonomous mechanical character in the Star Wars universe
 */
data class DroidDetails2Impl(
  override val __typename: String = "Droid",
  /**
   * This droid's primary function
   */
  override val primaryFunction: String?
) : DroidDetails2, GraphqlFragment {
  override fun marshaller(): ResponseFieldMarshaller {
    return ResponseFieldMarshaller { writer ->
      DroidDetails2Impl_ResponseAdapter.toResponse(writer, this)
    }
  }

  companion object {
    val FRAGMENT_DEFINITION: String = """
        |fragment DroidDetails2 on Droid {
        |  __typename
        |  primaryFunction
        |}
        """.trimMargin()
  }
}
