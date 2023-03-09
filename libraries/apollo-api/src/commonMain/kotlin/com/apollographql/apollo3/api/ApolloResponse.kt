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
     * [GraphQL errors](https://spec.graphql.org/October2021/#sec-Errors) returned by the server to let the client know that something
     * has gone wrong.
     *
     * If no GraphQL error was raised, [errors] is null. Else it's a non-empty list of errors indicating where the error(s) happened.
     *
     * Note that because GraphQL allows partial data, it is possible to have both [data] non null and [errors] non null.
     *
     * See also [exception] for exceptions happening before a valid GraphQL response could be received.
     */
    @JvmField
    val errors: List<Error>?,

    /**
     * An [ApolloException] if a valid GraphQL response wasn't received or `null` if a valid GraphQL response was received.
     * For example, `exception` is non null if there is a network failure or cache miss.
     * If `exception` is non null, [data] and [errors] will be null.
     *
     * See also [errors] for GraphQL errors returned by the server.
     */
    @JvmField
    val exception: ApolloException?,

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

    /**
     * Indicates that this [ApolloResponse] is the last [ApolloResponse] in a given [Flow] and that no
     * other items are expected.
     *
     * This is used as a hint by the watchers to make sure to subscribe before the last item is emitted.
     *
     * There can be false negatives where [isLast] is false if the producer does not know in advance if
     * other items are emitted. For an example, the CacheAndNetwork fetch policy doesn't emit the network
     * item if it fails.
     *
     * There must not be false positives. If [isLast] is true, no other items must follow.
     */
    @JvmField
    val isLast: Boolean,
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
      return when {
        exception != null -> throw exception
        hasErrors() -> throw ApolloException("The response has errors: $errors")
        else -> data ?: throw ApolloException("The server did not return any data")
      }
    }

  fun hasErrors(): Boolean = !errors.isNullOrEmpty()

  fun newBuilder(): Builder<D> {
    return Builder(operation, requestUuid, data)
        .errors(errors)
        .exception(exception)
        .extensions(extensions)
        .addExecutionContext(executionContext)
        .isLast(isLast)
  }

  class Builder<D : Operation.Data>(
      private val operation: Operation<D>,
      private var requestUuid: Uuid,
      private var data: D?,
  ) {
    private var executionContext: ExecutionContext = ExecutionContext.Empty
    private var errors: List<Error>? = null
    private var exception: ApolloException? = null
    private var extensions: Map<String, Any?>? = null
    private var isLast = false

    fun addExecutionContext(executionContext: ExecutionContext) = apply {
      this.executionContext = this.executionContext + executionContext
    }

    fun errors(errors: List<Error>?) = apply {
      this.errors = errors
    }

    fun exception(exception: ApolloException?) = apply {
      this.exception = exception
    }

    fun extensions(extensions: Map<String, Any?>?) = apply {
      this.extensions = extensions
    }

    fun requestUuid(requestUuid: Uuid) = apply {
      this.requestUuid = requestUuid
    }

    fun data(data: D?) = apply {
      this.data = data
    }

    fun isLast(isLast: Boolean) = apply {
      this.isLast = isLast
    }

    fun build(): ApolloResponse<D> {
      @Suppress("DEPRECATION")
      return ApolloResponse(
          operation = operation,
          requestUuid = requestUuid,
          data = data,
          executionContext = executionContext,
          extensions = extensions ?: emptyMap(),
          errors = errors,
          exception = exception,
          isLast = isLast,
      )
    }
  }
}
