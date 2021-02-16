// AUTO-GENERATED FILE. DO NOT MODIFY.
//
// This class was automatically generated by Apollo GraphQL plugin from the GraphQL queries it found.
// It should not be modified by hand.
//
package com.example.fragments_with_type_condition

import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.api.ResponseAdapterCache
import com.apollographql.apollo3.api.ResponseField
import com.apollographql.apollo3.api.internal.QueryDocumentMinifier
import com.apollographql.apollo3.api.internal.ResponseAdapter
import com.example.fragments_with_type_condition.adapter.TestQuery_ResponseAdapter
import com.example.fragments_with_type_condition.fragment.DroidDetails
import com.example.fragments_with_type_condition.fragment.HumanDetails
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
    val r2: R2?,
    val luke: Luke?
  ) : Operation.Data {
    /**
     * A character from the Star Wars universe
     */
    interface R2 {
      val __typename: String

      data class HumanR2(
        override val __typename: String,
        /**
         * What this human calls themselves
         */
        override val name: String,
        /**
         * Height in the preferred unit, default is meters
         */
        override val height: Double?
      ) : R2, HumanDetails

      data class DroidR2(
        override val __typename: String,
        /**
         * What others call this droid
         */
        override val name: String,
        /**
         * This droid's primary function
         */
        override val primaryFunction: String?
      ) : R2, DroidDetails

      data class OtherR2(
        override val __typename: String
      ) : R2

      companion object {
        fun R2.asHumanR2(): HumanR2? = this as? HumanR2

        fun R2.asDroidR2(): DroidR2? = this as? DroidR2
      }
    }

    /**
     * A character from the Star Wars universe
     */
    interface Luke {
      val __typename: String

      data class HumanLuke(
        override val __typename: String,
        /**
         * What this human calls themselves
         */
        override val name: String,
        /**
         * Height in the preferred unit, default is meters
         */
        override val height: Double?
      ) : Luke, HumanDetails

      data class DroidLuke(
        override val __typename: String,
        /**
         * What others call this droid
         */
        override val name: String,
        /**
         * This droid's primary function
         */
        override val primaryFunction: String?
      ) : Luke, DroidDetails

      data class OtherLuke(
        override val __typename: String
      ) : Luke

      companion object {
        fun Luke.asHumanLuke(): HumanLuke? = this as? HumanLuke

        fun Luke.asDroidLuke(): DroidLuke? = this as? DroidLuke
      }
    }
  }

  companion object {
    const val OPERATION_ID: String =
        "919cec7210259fa24fc6026fe680b96f357c14ebf3c8a734979dcfb819685d6a"

    val QUERY_DOCUMENT: String = QueryDocumentMinifier.minify(
          """
          |query TestQuery {
          |  r2: hero {
          |    __typename
          |    ...HumanDetails
          |    ...DroidDetails
          |  }
          |  luke: hero {
          |    __typename
          |    ...HumanDetails
          |    ...DroidDetails
          |  }
          |}
          |fragment HumanDetails on Human {
          |  __typename
          |  name
          |  height
          |}
          |fragment DroidDetails on Droid {
          |  __typename
          |  name
          |  primaryFunction
          |}
          """.trimMargin()
        )

    val OPERATION_NAME: String = "TestQuery"
  }
}
