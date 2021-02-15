// AUTO-GENERATED FILE. DO NOT MODIFY.
//
// This class was automatically generated by Apollo GraphQL plugin from the GraphQL queries it found.
// It should not be modified by hand.
//
package com.example.root_query_fragment

import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.Query
import com.apollographql.apollo.api.ResponseAdapterCache
import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.internal.QueryDocumentMinifier
import com.apollographql.apollo.api.internal.ResponseAdapter
import com.example.root_query_fragment.adapter.TestQuery_ResponseAdapter
import com.example.root_query_fragment.fragment.QueryFragment
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
    ResponseField.FieldSet("Query", TestQuery_ResponseAdapter.QueryData.RESPONSE_FIELDS),
    ResponseField.FieldSet(null, TestQuery_ResponseAdapter.OtherData.RESPONSE_FIELDS),
  )
  /**
   * The query type, represents all of the entry points into our object graph
   */
  interface Data : Operation.Data {
    val __typename: String

    data class QueryData(
      override val __typename: String,
      override val hero: Hero?
    ) : Data, QueryFragment {
      /**
       * A character from the Star Wars universe
       */
      data class Hero(
        /**
         * The name of the character
         */
        override val name: String
      ) : QueryFragment.Hero
    }

    data class OtherData(
      override val __typename: String
    ) : Data

    companion object {
      fun Data.asQueryData(): QueryData? = this as? QueryData
    }
  }

  companion object {
    const val OPERATION_ID: String =
        "f2287d7a8933207536dba2321db795487257ae1c8f5a9f0577d02361c0117ae5"

    val QUERY_DOCUMENT: String = QueryDocumentMinifier.minify(
          """
          |query TestQuery {
          |  __typename
          |  ...QueryFragment
          |}
          |fragment QueryFragment on Query {
          |  __typename
          |  hero {
          |    name
          |  }
          |}
          """.trimMargin()
        )

    val OPERATION_NAME: String = "TestQuery"
  }
}
