package com.apollographql.apollo3.network.ws.incubating

import com.apollographql.apollo3.api.json.ApolloJsonElement
import com.apollographql.apollo3.exception.ApolloException

internal interface OperationListener {
  /**
   * A response was received
   *
   * [response] is the Kotlin representation of a GraphQL response.
   *
   * ```kotlin
   * mapOf(
   *   "data" to ...
   *   "errors" to listOf(...)
   * )
   * ```
   */
  fun onResponse(response: ApolloJsonElement)

  /**
   * The operation terminated successfully. No future calls to this listener are made.
   */
  fun onComplete()

  /**
   * The server sent an error. That error may be terminal in which case, no future calls to this listener are made.
   */
  fun onError(payload: ApolloJsonElement, terminal: Boolean)

  /**
   * The transport failed. No future calls to this listener are made.
   */
  fun onTransportError(cause: ApolloException)
}


