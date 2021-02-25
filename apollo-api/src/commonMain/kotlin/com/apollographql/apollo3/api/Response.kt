package com.apollographql.apollo3.api

import kotlin.jvm.JvmStatic

/**
 * Represents a GraphQL response. GraphQL responses can be be partial responses so it is valid to have both data != null and errors
 */
data class Response<D : Operation.Data>(
    /**
     * GraphQL operation this response represents of
     */
    val operation: Operation<*>,

    /**
     * Parsed response of GraphQL [operation] execution.
     * Can be `null` in case if [operation] execution failed.
     */
    val data: D?,

    /**
     * GraphQL [operation] execution errors returned by the server to let client know that something has gone wrong.
     */
    val errors: List<Error>? = null,

    /**
     * Indicates if response is resolved from the cache.
     * // TODO remove as it is now in the ExecutionContext
     */
    val isFromCache: Boolean = false,

    /**
     * Extensions of GraphQL protocol, arbitrary map of key [String] / value [Any] sent by server along with the response.
     */
    val extensions: Map<String, Any?> = emptyMap(),

    /**
     * The context of GraphQL [operation] execution.
     */
    val executionContext: ExecutionContext = ExecutionContext.Empty
) {
  fun hasErrors(): Boolean = !errors.isNullOrEmpty()
}
