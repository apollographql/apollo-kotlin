package com.apollographql.apollo.api

import kotlin.js.JsName

/**
 * Represents a GraphQL operation (mutation, query or subscription).
 */
interface Operation<D : Operation.Data> : Executable<D> {
  /**
   * The GraphQL operation String to be sent to the server. This might differ from the input `*.graphql` file with:
   * - whitespaces removed
   * - Apollo client directives like `@catch` removed
   * - `typename` fields added for polymorphic/fragment cases
   */
  fun document(): String

  /**
   * The GraphQL operation name as in the `*.graphql` file.
   */
  @JsName("operationName")
  fun name(): String

  /**
   * An unique identifier for the operation. Used for Automatic Persisted Queries.
   * You can customize it with a [OperationIdGenerator]
   */
  @JsName("operationId")
  fun id(): String

  /**
   * Marker interface for generated models built from data returned by the server in response to this operation.
   */
  interface Data : Executable.Data
}
