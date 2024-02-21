package com.apollographql.apollo3.network.websocket

internal interface WebSocketOperationListener {
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
  fun onResponse(response: Any?)

  /**
   * The operation terminated successfully. No future calls to this listener will be made
   */
  fun onComplete()

  /**
   * The operation cannot be executed or failed. No future calls to this listener will be made.
   *
   * This may happen for an example if there are validation errors
   */
  fun onError(throwable: Throwable)
}


