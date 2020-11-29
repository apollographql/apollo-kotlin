// AUTO-GENERATED FILE. DO NOT MODIFY.
//
// This class was automatically generated by Apollo GraphQL plugin from the GraphQL queries it found.
// It should not be modified by hand.
//
package com.example.simple_fragment.fragment

import com.apollographql.apollo.api.GraphqlFragment
import com.apollographql.apollo.api.internal.ResponseFieldMapper
import com.apollographql.apollo.api.internal.ResponseFieldMarshaller
import com.apollographql.apollo.api.internal.ResponseReader
import kotlin.String
import kotlin.Suppress

/**
 * Fragment with Java / Kotlin docs generation
 * with multi lines support
 */
@Suppress("NAME_SHADOWING", "UNUSED_ANONYMOUS_PARAMETER", "LocalVariableName",
    "RemoveExplicitTypeArguments", "NestedLambdaShadowedImplicitParameter", "PropertyName",
    "RemoveRedundantQualifierName")
internal interface HeroDetail : GraphqlFragment {
  val __typename: String

  /**
   * Fragment with Java / Kotlin docs generation
   */
  interface Human : HeroDetail, HumanDetail {
    override val __typename: String

    /**
     * What this human calls themselves
     */
    override val name: String

    override fun marshaller(): ResponseFieldMarshaller
  }

  companion object {
    val FRAGMENT_DEFINITION: String = """
        |fragment HeroDetails on Character {
        |  __typename
        |  ...HumanDetails
        |}
        """.trimMargin()

    operator fun invoke(reader: ResponseReader): HeroDetail {
      return HeroDetailsImpl_ResponseAdapter.fromResponse(reader)
    }

    fun Mapper(): ResponseFieldMapper<HeroDetail> {
      return ResponseFieldMapper { reader ->
        HeroDetailsImpl_ResponseAdapter.fromResponse(reader)
      }
    }
  }
}
