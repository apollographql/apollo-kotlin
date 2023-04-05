package com.apollographql.apollo3.api

import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.exception.ApolloGraphQLException
import com.apollographql.apollo3.exception.DefaultApolloException
import com.benasher44.uuid.Uuid
import kotlin.jvm.JvmField
import kotlin.jvm.JvmName

/**
 * Represents a GraphQL response. GraphQL responses can be partial responses, so it is valid to have both data != null and exception != null
 *
 * Valid states are:
 * - data != null && exception == null: complete data with no error
 * - data == null && exception != null: no data, a network error or operation error happened
 * - data != null && exception != null: partial data with field errors
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
     *
     * See also [exception]
     */
    @JvmField
    val data: D?,

    /**
     * [GraphQL errors](https://spec.graphql.org/October2021/#sec-Errors) returned by the server to let the client know that something
     * has gone wrong.
     *
     * If no GraphQL error was raised, [errors] is null. Else it's a non-empty list of errors indicating where the error(s) happened.
     *
     * Note that because GraphQL allows partial data, it is possible to have both [data] non-null and [errors] non-null.
     *
     * See also [exception]
     */
    @JvmField
    val errors: List<Error>?,

    exception: ApolloException?,

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
   * An [ApolloException] if a complete GraphQL response wasn't received, an instance of [ApolloGraphQLException] if GraphQL
   * errors were return or another instance of [ApolloException] if a network, parsing, caching or other error happened.
   *
   * For example, `exception` is non-null if there is a network failure or cache miss.
   *
   * See also [data]
   */
  @JvmField
  val exception: ApolloException? = when  {
    exception != null -> exception
    !errors.isNullOrEmpty() -> ApolloGraphQLException(errors)
    data == null -> DefaultApolloException("No data and no error was returned")
    else -> null
  }

  /**
   * A shorthand property to get a non-nullable `data` if handling partial data is **not** important
   *
   * Note: A future version could use [Definitely non nullable types](https://github.com/Kotlin/KEEP/pull/269)
   * to implement something like `ApolloResponse<D>.assertNoErrors(): ApolloResponse<D & Any>`
   */
  @get:JvmName("dataAssertNoErrors")
  @Deprecated(message = "Use dataOrThrow instead", replaceWith = ReplaceWith("dataOrThrow()"))
  val dataAssertNoErrors: D
    get() {
      return dataOrThrow()
    }

  /**
   * Return [data] if not null or throws [exception] else
   */
  fun dataOrThrow(): D = data ?: throw exception!!

  fun hasErrors(): Boolean = !errors.isNullOrEmpty()

  fun newBuilder(): Builder<D> {
    return Builder(operation, requestUuid, data, errors, extensions, exception)
        .addExecutionContext(executionContext)
        .isLast(isLast)
  }

  class Builder<D : Operation.Data> internal constructor(
      private val operation: Operation<D>,
      private var requestUuid: Uuid,
      private var data: D?,
      private var errors: List<Error>?,
      private var extensions: Map<String, Any?>?,
      private var exception: ApolloException?
  ) {
    private var executionContext: ExecutionContext = ExecutionContext.Empty
    private var isLast = false

    /**
     * Constructs a successful response with a valid data, no errors nor extensions
     */
    constructor(
        operation: Operation<D>,
        requestUuid: Uuid,
        data: D,
    ): this(operation, requestUuid, data, null, null, null)

    /**
     * Constructs a response from data, errors and extensions
     *
     * If there are GraphQL errors, they will also be forwarded to [exception] so that the caller can do all the
     * checking in a single place
     */
    constructor(
        operation: Operation<D>,
        requestUuid: Uuid,
        data: D?,
        errors: List<Error>?,
        extensions: Map<String, Any?>?,
    ): this(operation, requestUuid, data, errors, extensions, null)

    /**
     * Constructs an exception response
     */
    constructor(
        operation: Operation<D>,
        requestUuid: Uuid,
        exception: ApolloException
    ): this(operation, requestUuid, null, null, null, exception)

    fun addExecutionContext(executionContext: ExecutionContext) = apply {
      this.executionContext = this.executionContext + executionContext
    }

    fun data(data: D?) = apply {
      this.data = data
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

    fun isLast(isLast: Boolean) = apply {
      this.isLast = isLast
    }

    fun build(): ApolloResponse<D> {
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
