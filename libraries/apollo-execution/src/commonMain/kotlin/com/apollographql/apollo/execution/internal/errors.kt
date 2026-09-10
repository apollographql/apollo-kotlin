package com.apollographql.apollo.execution.internal

import com.apollographql.apollo.api.Error
import com.apollographql.apollo.ast.Issue
import com.apollographql.apollo.execution.GraphQLResponse
import com.apollographql.apollo.execution.SubscriptionResponse
import kotlinx.coroutines.flow.flowOf

internal fun graphQLError(message: String) = Error.Builder(message).build()
internal fun singleGraphQLError(message: String) = listOf(Error.Builder(message).build())

internal fun graphqlErrorResponse(message: String) = graphqlErrorResponse(listOf(graphQLError(message)))
internal fun graphqlErrorResponse(errors: List<Error>) = GraphQLResponse.Builder().errors(errors).build()
internal fun subscriptionError(message: String) = flowOf(SubscriptionResponse(graphqlErrorResponse(message)))

internal fun List<Issue>.toErrors(): List<Error> {
  return map {
    Error.Builder(
      message = it.message,
    ).locations(
      listOf(Error.Location(it.sourceLocation!!.line, it.sourceLocation!!.column))
    ).build()
  }
}
