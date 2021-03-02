package com.apollographql.apollo3.api

import com.benasher44.uuid.Uuid

/**
 * Represents a GraphQL response. GraphQL responses can be be partial responses so it is valid to have both data != null and errors
 */
data class ApolloResponse<out D : Operation.Data>(
    val requestUuid: Uuid,

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
     * This can either be null or empty depending what you server sends back
     */
    val errors: List<Error>? = null,

    /**
     * Extensions of GraphQL protocol, arbitrary map of key [String] / value [Any] sent by server along with the response.
     */
    val extensions: Map<String, Any?> = emptyMap(),

    /**
     * The context of GraphQL [operation] execution.
     * This can contain additional data contributed by interceptors.
     */
    val executionContext: ExecutionContext = ExecutionContext.Empty
) {
  fun hasErrors(): Boolean = !errors.isNullOrEmpty()
}
