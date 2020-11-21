package com.apollographql.apollo.compiler.ir

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Operation(
    val operationName: String,
    val packageName: String,
    val operationType: String,
    val description: String,
    val variables: List<Variable>,
    val source: String,
    val sourceWithFragments: String,
    val fields: List<Field>,
    val filePath: String,
    val fragments: List<FragmentRef>,
    val inlineFragments: List<InlineFragment>,
    val fragmentsReferenced: List<String>
) {
  fun isMutation() = operationType == TYPE_MUTATION

  fun isQuery() = operationType == TYPE_QUERY

  fun isSubscription() = operationType == TYPE_SUBSCRIPTION

  companion object {
    const val DATA_TYPE_NAME = "Data"
    const val TYPE_MUTATION = "mutation"
    const val TYPE_QUERY = "query"
    const val TYPE_SUBSCRIPTION = "subscription"
  }
}
