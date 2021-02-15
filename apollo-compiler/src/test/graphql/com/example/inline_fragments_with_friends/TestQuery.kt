// AUTO-GENERATED FILE. DO NOT MODIFY.
//
// This class was automatically generated by Apollo GraphQL plugin from the GraphQL queries it found.
// It should not be modified by hand.
//
package com.example.inline_fragments_with_friends

import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.Query
import com.apollographql.apollo.api.ResponseAdapterCache
import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.internal.QueryDocumentMinifier
import com.apollographql.apollo.api.internal.ResponseAdapter
import com.example.inline_fragments_with_friends.adapter.TestQuery_ResponseAdapter
import com.example.inline_fragments_with_friends.type.Episode
import kotlin.Double
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

      data class HumanHero(
        override val __typename: String,
        /**
         * The name of the character
         */
        override val name: String,
        /**
         * Height in the preferred unit, default is meters
         */
        val height: Double?,
        /**
         * This human's friends, or an empty list if they have none
         */
        val friends: List<Friends?>?
      ) : Hero {
        /**
         * A character from the Star Wars universe
         */
        data class Friends(
          /**
           * The movies this character appears in
           */
          val appearsIn: List<Episode?>
        )
      }

      data class DroidHero(
        override val __typename: String,
        /**
         * The name of the character
         */
        override val name: String,
        /**
         * This droid's primary function
         */
        val primaryFunction: String?,
        /**
         * This droid's friends, or an empty list if they have none
         */
        val friends: List<Friends?>?
      ) : Hero {
        /**
         * A character from the Star Wars universe
         */
        data class Friends(
          /**
           * The ID of the character
           */
          val id: String
        )
      }

      data class OtherHero(
        override val __typename: String,
        /**
         * The name of the character
         */
        override val name: String
      ) : Hero

      companion object {
        fun Hero.asHumanHero(): HumanHero? = this as? HumanHero

        fun Hero.asDroidHero(): DroidHero? = this as? DroidHero
      }
    }
  }

  companion object {
    const val OPERATION_ID: String =
        "43bfea6068cd77041d723551dd119f0676f6c333620dd281a668eca49d14fcb5"

    val QUERY_DOCUMENT: String = QueryDocumentMinifier.minify(
          """
          |query TestQuery {
          |  hero {
          |    __typename
          |    name
          |    ... on Human {
          |      height
          |      friends {
          |        appearsIn
          |      }
          |    }
          |    ... on Droid {
          |      primaryFunction
          |      friends {
          |        id
          |      }
          |    }
          |  }
          |}
          """.trimMargin()
        )

    val OPERATION_NAME: String = "TestQuery"
  }
}
