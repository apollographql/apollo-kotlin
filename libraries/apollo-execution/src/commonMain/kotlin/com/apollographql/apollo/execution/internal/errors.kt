package com.apollographql.apollo.execution.internal

import com.apollographql.apollo.api.Error
import com.apollographql.apollo.ast.Issue
import com.apollographql.apollo.execution.GraphQLResponse

internal fun graphQLError(message: String) = Error.Builder(message).build()
internal fun singleGraphQLError(message: String) = listOf(Error.Builder(message).build())

internal fun graphqlErrorResponse(message: String) = GraphQLResponse.Builder().errors(listOf(graphQLError(message))).build()

internal fun List<Issue>.toErrors(): List<Error> {
  return map {
    Error.Builder(
      message = it.message,
    ).locations(
      listOf(Error.Location(it.sourceLocation!!.line, it.sourceLocation!!.column))
    ).build()
  }
}
