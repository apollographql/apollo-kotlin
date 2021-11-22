package com.apollographql.apollo3.api

import com.apollographql.apollo3.api.json.JsonWriter
import kotlin.js.JsName

/**
 * Represents a GraphQL operation (mutation, query or subscription).
 */
interface Operation<D : Operation.Data> : Executable<D> {
  /**
   * The GraphQL operation String to be sent to the server. This might differ from the input `*.graphql` file with:
   * - whitespaces removed
   * - Apollo client directives like `@nonnull` removed
   * - `typename` fields added for polymorphic/fragment cases
   */
  fun document(): String

  /**
   * The GraphQL operation name as in the `*.graphql` file.
   */
  fun name(): String

  /**
   * An unique identifier for the operation. Used for Automatic Persisted Queries.
   * You can customize it with a [OperationIdGenerator]
   */
  @JsName("operationId")
  fun id(): String

  override fun adapter(): Adapter<D>

  override fun serializeVariables(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters)

  override fun selections(): List<CompiledSelection>

  /**
   * Marker interface for generated models built from data returned by the server in response to this operation.
   */
  interface Data : Executable.Data
}
