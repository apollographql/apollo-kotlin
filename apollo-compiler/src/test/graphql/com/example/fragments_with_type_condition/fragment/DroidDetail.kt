// AUTO-GENERATED FILE. DO NOT MODIFY.
//
// This class was automatically generated by Apollo GraphQL plugin from the GraphQL queries it found.
// It should not be modified by hand.
//
package com.example.fragments_with_type_condition.fragment

import com.apollographql.apollo.api.GraphqlFragment
import com.apollographql.apollo.api.internal.ResponseFieldMapper
import com.apollographql.apollo.api.internal.ResponseReader
import kotlin.String
import kotlin.Suppress

@Suppress("NAME_SHADOWING", "UNUSED_ANONYMOUS_PARAMETER", "LocalVariableName",
    "RemoveExplicitTypeArguments", "NestedLambdaShadowedImplicitParameter", "PropertyName",
    "RemoveRedundantQualifierName")
interface DroidDetail : GraphqlFragment {
  val __typename: String

  /**
   * What others call this droid
   */
  val name: String

  /**
   * This droid's primary function
   */
  val primaryFunction: String?

  companion object {
    val FRAGMENT_DEFINITION: String = """
        |fragment DroidDetails on Droid {
        |  __typename
        |  name
        |  primaryFunction
        |}
        """.trimMargin()

    operator fun invoke(reader: ResponseReader): DroidDetail {
      return DroidDetailsImpl_ResponseAdapter.fromResponse(reader)
    }

    fun Mapper(): ResponseFieldMapper<DroidDetail> {
      return ResponseFieldMapper { reader ->
        DroidDetailsImpl_ResponseAdapter.fromResponse(reader)
      }
    }
  }
}
