// AUTO-GENERATED FILE. DO NOT MODIFY.
//
// This class was automatically generated by Apollo GraphQL plugin from the GraphQL queries it found.
// It should not be modified by hand.
//
package com.example.typename_always_first

import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.Query
import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.internal.QueryDocumentMinifier
import com.apollographql.apollo.api.internal.ResponseAdapter
import com.example.typename_always_first.adapter.TestQuery_ResponseAdapter
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

  override fun adapter(customScalarAdapters: CustomScalarAdapters): ResponseAdapter<Data> {
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
    val hero: Hero?,
    val __typename: String = "Query"
  ) : Operation.Data {
    /**
     * A character from the Star Wars universe
     */
    interface Hero {
      val __typename: String

      data class HumanHero(
        override val __typename: String,
        /**
         * Height in the preferred unit, default is meters
         */
        val height: Double?
      ) : Hero

      data class DroidHero(
        override val __typename: String,
        /**
         * What others call this droid
         */
        val name: String,
        /**
         * This droid's primary function
         */
        val primaryFunction: String?
      ) : Hero

      data class OtherHero(
        override val __typename: String
      ) : Hero

      companion object {
        fun Hero.asHumanHero(): HumanHero? = this as? HumanHero

        fun Hero.asDroidHero(): DroidHero? = this as? DroidHero
      }
    }
  }

  companion object {
    const val OPERATION_ID: String =
        "5351cc2f9dea89f8d8d9513a08a18eee20324eaba674211bf21f9c01cba9fdea"

    val QUERY_DOCUMENT: String = QueryDocumentMinifier.minify(
          """
          |query TestQuery {
          |  hero {
          |    __typename
          |    ... on Human {
          |      height
          |      __typename
          |    }
          |    ... on Droid {
          |      name
          |      __typename
          |      primaryFunction
          |    }
          |  }
          |  __typename
          |}
          """.trimMargin()
        )

    val OPERATION_NAME: String = "TestQuery"
  }
}
