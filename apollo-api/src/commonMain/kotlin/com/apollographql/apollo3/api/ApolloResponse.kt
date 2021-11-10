package com.apollographql.apollo3.api

import com.apollographql.apollo3.exception.ApolloException
import com.benasher44.uuid.Uuid
import kotlin.jvm.JvmField
import kotlin.jvm.JvmName

/**
 * Represents a GraphQL response. GraphQL responses can be be partial responses so it is valid to have both data != null and errors
 */
class ApolloResponse<out D : Operation.Data>(
    @JvmField
    val requestUuid: Uuid,

    /**
     * The GraphQL operation this response represents
     */
    @JvmField
    val operation: Operation<*>,

    /**
     * Parsed response of GraphQL [operation] execution.
     * Can be `null` in case if [operation] execution failed.
     */
    @JvmField
    val data: D?,

    /**
     * GraphQL [operation] execution errors returned by the server to let client know that something has gone wrong.
     * This can either be null or empty depending what you server sends back
     */
    @JvmField
    val errors: List<Error>? = null,

    /**
     * Extensions of GraphQL protocol, arbitrary map of key [String] / value [Any] sent by server along with the response.
     */
    @JvmField
    val extensions: Map<String, Any?> = emptyMap(),

    /**
     * The context of GraphQL [operation] execution.
     * This can contain additional data contributed by interceptors.
     */
    @JvmField
    val executionContext: ExecutionContext = ExecutionContext.Empty,
) {

  /**
   * A shorthand property to get a non-nullable if handling partial data is not important
   */
  @Deprecated("Please use dataAssertNoErrors methods instead. This will be removed in v3.0.0.",
  ReplaceWith("dataAssertNoErrors"))
  @get:JvmName("dataOrThrow")
  val dataOrThrow: D
    get() {
      return if (hasErrors()) {
        throw ApolloException("The response has errors: $errors")
      } else {
        data ?: throw  ApolloException("The server did not return any data")
      }
    }

  /**
   * A shorthand property to get a non-nullable if handling partial data is **not** important
   *
   * Note: A future version could use [Definitely non nullable types](https://github.com/Kotlin/KEEP/pull/269)
   * to implement something like `ApolloResponse<D>.assertNoErrors(): ApolloResponse<D & Any>`
   */
  @get:JvmName("dataAssertNoErrors")
  val dataAssertNoErrors: D
    get() {
      return if (hasErrors()) {
        throw ApolloException("The response has errors: $errors")
      } else {
        data ?: throw  ApolloException("The server did not return any data")
      }
    }

  fun hasErrors(): Boolean = !errors.isNullOrEmpty()

  fun copy(
      requestUuid: Uuid = this.requestUuid,
      operation: Operation<*> = this.operation,
      data: Any? = this.data,
      errors: List<Error>? = this.errors,
      extensions: Map<String, Any?> = this.extensions,
      executionContext: ExecutionContext = this.executionContext,
  ): ApolloResponse<D> {
    @Suppress("UNCHECKED_CAST")
    return ApolloResponse(
        requestUuid,
        operation,
        data as D?,
        errors,
        extensions,
        executionContext
    )
  }

  fun withExecutionContext(executionContext: ExecutionContext) = copy(executionContext = executionContext)
}
