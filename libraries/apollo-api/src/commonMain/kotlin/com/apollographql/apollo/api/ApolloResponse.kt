package com.apollographql.apollo.api

import com.apollographql.apollo.annotations.ApolloDeprecatedSince
import com.apollographql.apollo.exception.ApolloException
import com.apollographql.apollo.exception.ApolloGraphQLException
import com.apollographql.apollo.exception.DefaultApolloException
import com.apollographql.apollo.exception.NoDataException
import com.benasher44.uuid.Uuid
import kotlin.jvm.JvmField
import kotlin.jvm.JvmName

/**
 * Represents a GraphQL response or an exception if no GraphQL response is received.
 *
 * - [data] contains the parsed data returned in the response
 * - [errors] contains any GraphQL errors returned in the response
 * - [exception] contains an exception if a GraphQL response wasn't received
 *
 * GraphQL responses can contain partial data, so it is possible to have both [data] != null and [errors] != null. An [ApolloResponse] may be in any of these states:
 * - exception == null && data != null && errors == null: complete data with no errors
 * - exception == null && data != null && errors != null: partial data with GraphQL errors
 * - exception == null && data == null && errors != null: no data, only GraphQL errors
 * - exception == null && data == null && errors == null: no data and no errors - while technically possible, this should not happen with a spec-compliant server
 * - exception != null && data == null && errors == null: no GraphQL response was received due to a network error or otherwise
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
     * [data] is non-null if some data is received from the server.
     * [data] might contain partial data in case of field errors
     *
     * @see [errors]
     * @see [exception]
     * @see [dataOrThrow]
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

    /**
     * An [ApolloException] if a GraphQL response wasn't received.
     *
     * For example, `exception` is non-null in those cases:
     * - network failure
     * - cache miss
     * - parsing error
     *
     * See also [data]
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
     * other items are emitted.
     *
     * There must not be false positives. If [isLast] is true, no other items must follow.
     */
    @JvmField
    val isLast: Boolean,
) {

  /**
   * A shorthand property to get a non-nullable `data` if handling partial data is **not** important
   */
  @get:JvmName("dataAssertNoErrors")
  val dataAssertNoErrors: D
    get() {
      return when {
        hasErrors() -> throw ApolloGraphQLException(errors!!.first())
        exception != null -> throw DefaultApolloException("An exception happened", exception)
        else -> dataOrThrow()
      }
    }

  /**
   * Return [data] if not null or throws [NoDataException] else
   */
  fun dataOrThrow(): D {
    return data ?: throw NoDataException(cause = exception)
  }

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
     * Construct a new [Builder] that doesn't contain data, errors nor exception
     *
     * While possible, this case is probably symptomatic of a buggy GraphQL implementation
     * that returned no data while not setting any error
     */
    constructor(
        operation: Operation<D>,
        requestUuid: Uuid,
    ): this(operation, requestUuid, null, null, null, null)

    @Deprecated("Use 2 params constructor instead", ReplaceWith("Builder(operation = operation, requestUuid = requestUuid).data(data = data)"))
    @ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
    constructor(
        operation: Operation<D>,
        requestUuid: Uuid,
        data: D?
    ): this(operation, requestUuid, data, null, null, null)

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
