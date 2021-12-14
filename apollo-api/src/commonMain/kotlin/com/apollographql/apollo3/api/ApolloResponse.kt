package com.apollographql.apollo3.api

import com.apollographql.apollo3.exception.ApolloException
import com.benasher44.uuid.Uuid
import kotlin.jvm.JvmField
import kotlin.jvm.JvmName

/**
 * Represents a GraphQL response. GraphQL responses can be be partial responses so it is valid to have both data != null and errors
 */
class ApolloResponse<D : Operation.Data>
private constructor(
    @JvmField
    val requestUuid: Uuid,

    /**
     * The GraphQL operation this response represents
     */
    @JvmField
    val operation: Operation<D>,

    /**
     * Parsed response of GraphQL [operation] execution.
     * Can be `null` in case if [operation] execution failed.
     */
    @JvmField
    val data: D?,

    /**
     * GraphQL [operation] execution errors returned by the server to let client know that something has gone wrong.
     * This can either be null or empty depending on what your server sends back
     */
    @JvmField
    val errors: List<Error>?,

    /**
     * Extensions of GraphQL protocol, arbitrary map of key [String] / value [Any] sent by server along with the response.
     */
    @JvmField
    val extensions: Map<String, Any?>,

    /**
     * The context of GraphQL [operation] execution.
     * This can contain additional data contributed by interceptors.
     */
    @JvmField
    val executionContext: ExecutionContext,
) {

  /**
   * A shorthand property to get a non-nullable `data` if handling partial data is **not** important
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

  fun newBuilder(): Builder<D> {
    return Builder(operation, requestUuid, data)
        .errors(errors)
        .extensions(extensions)
        .addExecutionContext(executionContext)
  }

  class Builder<D : Operation.Data>(
      private val operation: Operation<D>,
      private var requestUuid: Uuid,
      private val data: D?,
  ) {
    private var executionContext: ExecutionContext = ExecutionContext.Empty
    private var errors: List<Error>? = null
    private var extensions: Map<String, Any?>? = null

    fun addExecutionContext(executionContext: ExecutionContext) = apply {
      this.executionContext = this.executionContext + executionContext
    }

    fun errors(errors: List<Error>?) = apply {
      this.errors = errors
    }

    fun extensions(extensions: Map<String, Any?>?) = apply {
      this.extensions = extensions
    }

    fun requestUuid(requestUuid: Uuid) = apply {
      this.requestUuid = requestUuid
    }

    fun build(): ApolloResponse<D> {
      @Suppress("DEPRECATION")
      return ApolloResponse(
          operation = operation,
          requestUuid = requestUuid,
          data = data,
          executionContext = executionContext,
          extensions = extensions ?: emptyMap(),
          errors = errors ,
      )
    }
  }
}

@Deprecated("This is a helper typealias to help migrating to 3.x " +
    "and will be removed in a future version",
    ReplaceWith("ApolloResponse"),
    DeprecationLevel.ERROR
)
typealias Response<D> = ApolloResponse<D>
