package com.apollographql.apollo.network.websocket.internal

import com.apollographql.apollo.api.json.ApolloJsonElement
import com.apollographql.apollo.exception.ApolloException

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
   * The server sent an error for this operation.
   * That error is terminal. No future calls to this listener are made.
   */
  fun onError(payload: ApolloJsonElement)

  /**
   * The transport failed. No future calls to this listener are made.
   */
  fun onTransportError(cause: ApolloException)
}