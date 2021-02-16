// AUTO-GENERATED FILE. DO NOT MODIFY.
//
// This class was automatically generated by Apollo GraphQL plugin from the GraphQL queries it found.
// It should not be modified by hand.
//
package com.example.starships

import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.api.ResponseAdapterCache
import com.apollographql.apollo3.api.ResponseField
import com.apollographql.apollo3.api.internal.InputFieldMarshaller
import com.apollographql.apollo3.api.internal.QueryDocumentMinifier
import com.apollographql.apollo3.api.internal.ResponseAdapter
import com.example.starships.adapter.TestQuery_ResponseAdapter
import kotlin.Any
import kotlin.Double
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.jvm.Transient

@Suppress("NAME_SHADOWING", "UNUSED_ANONYMOUS_PARAMETER", "LocalVariableName",
    "RemoveExplicitTypeArguments", "NestedLambdaShadowedImplicitParameter", "PropertyName",
    "RemoveRedundantQualifierName")
data class TestQuery(
  val id: String
) : Query<TestQuery.Data> {
  @Transient
  private val variables: Operation.Variables = object : Operation.Variables() {
    override fun valueMap(): Map<String, Any?> = mutableMapOf<String, Any?>().apply {
      this["id"] = this@TestQuery.id
    }

    override fun marshaller(): InputFieldMarshaller {
      return InputFieldMarshaller.invoke { writer ->
        writer.writeString("id", this@TestQuery.id)
      }
    }
  }

  override fun operationId(): String = OPERATION_ID

  override fun queryDocument(): String = QUERY_DOCUMENT

  override fun variables(): Operation.Variables = variables

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
    val starship: Starship?
  ) : Operation.Data {
    data class Starship(
      /**
       * The ID of the starship
       */
      val id: String,
      /**
       * The name of the starship
       */
      val name: String,
      val coordinates: List<List<Double>>?
    )
  }

  companion object {
    const val OPERATION_ID: String =
        "ec95d84c104260ea1c99e15341279aaabe98b7364279c2886a9ffe9adfeefb7f"

    val QUERY_DOCUMENT: String = QueryDocumentMinifier.minify(
          """
          |query TestQuery(${'$'}id: ID!) {
          |  starship(id: ${'$'}id) {
          |    id
          |    name
          |    coordinates
          |  }
          |}
          """.trimMargin()
        )

    val OPERATION_NAME: String = "TestQuery"
  }
}
