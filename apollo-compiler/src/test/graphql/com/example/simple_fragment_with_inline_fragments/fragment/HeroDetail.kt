// AUTO-GENERATED FILE. DO NOT MODIFY.
//
// This class was automatically generated by Apollo GraphQL plugin from the GraphQL queries it found.
// It should not be modified by hand.
//
package com.example.simple_fragment_with_inline_fragments.fragment

import com.apollographql.apollo.api.GraphqlFragment
import com.apollographql.apollo.api.internal.ResponseFieldMapper
import com.apollographql.apollo.api.internal.ResponseReader
import com.example.simple_fragment_with_inline_fragments.fragment.adapter.HeroDetailsImpl_ResponseAdapter
import kotlin.Double
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List

@Suppress("NAME_SHADOWING", "UNUSED_ANONYMOUS_PARAMETER", "LocalVariableName",
    "RemoveExplicitTypeArguments", "NestedLambdaShadowedImplicitParameter", "PropertyName",
    "RemoveRedundantQualifierName")
interface HeroDetail : GraphqlFragment {
  val __typename: String

  /**
   * The name of the character
   */
  val name: String

  /**
   * The friends of the character, or an empty list if they have none
   */
  val friends: List<Friend?>?

  /**
   * A character from the Star Wars universe
   */
  interface Friend {
    val __typename: String

    /**
     * The name of the character
     */
    val name: String

    interface Human : Friend {
      override val __typename: String

      /**
       * The name of the character
       */
      override val name: String

      /**
       * Height in the preferred unit, default is meters
       */
      val height: Double?
    }

    interface Droid : Friend {
      override val __typename: String

      /**
       * The name of the character
       */
      override val name: String

      /**
       * This droid's primary function
       */
      val primaryFunction: String?
    }

    companion object {
      fun Friend.asHuman(): Human? = this as? Human

      fun Friend.asDroid(): Droid? = this as? Droid
    }
  }

  companion object {
    val FRAGMENT_DEFINITION: String = """
        |fragment HeroDetails on Character {
        |  __typename
        |  name
        |  friends {
        |    __typename
        |    name
        |    ... on Human {
        |      height
        |    }
        |    ... on Droid {
        |      primaryFunction
        |    }
        |  }
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
