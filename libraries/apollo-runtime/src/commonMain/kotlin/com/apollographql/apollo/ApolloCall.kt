package com.apollographql.apollo

import com.apollographql.apollo.annotations.ApolloDeprecatedSince
import com.apollographql.apollo.annotations.ApolloExperimental
import com.apollographql.apollo.api.ApolloRequest
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.ExecutionContext
import com.apollographql.apollo.api.MutableExecutionOptions
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.http.HttpHeader
import com.apollographql.apollo.api.http.HttpMethod
import com.apollographql.apollo.exception.ApolloException
import com.apollographql.apollo.exception.DefaultApolloException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList

/**
 * An [ApolloCall] is a thin class that binds an [ApolloRequest] with its [ApolloClient]. It offers a fluent way to configure the [ApolloRequest].
 *
 * Contrary to an [ApolloRequest], an [ApolloCall] doesn't have a request id and a new request id is allocated every time [execute] or [toFlow] is called.
 *
 * - call [execute] for simple cases that expect a single response:
 *
 * ```
 * val response = apolloClient.query(myQuery).fetchPolicy(CacheOnly).execute()
 * ```
 *
 *
 * - call [toFlow] for other cases that expect multiple [ApolloResponse] like subscriptions and `@defer`:
 *
 * ```
 * apolloClient.subscription(mySubscription).toFlow().collect { response ->
 *   println(response.data)
 * }
 * ```
 *
 * @see execute
 * @see toFlow
 */
class ApolloCall<D : Operation.Data> internal constructor(
    internal val apolloClient: ApolloClient,
    private val requestBuilder: ApolloRequest.Builder<D>,
) : MutableExecutionOptions<ApolloCall<D>> {

  internal constructor(apolloClient: ApolloClient, operation: Operation<D>) : this(apolloClient, ApolloRequest.Builder(operation))

  val operation: Operation<D> get() = requestBuilder.operation
  override val executionContext: ExecutionContext get() = requestBuilder.executionContext
  override val httpMethod: HttpMethod? get() = requestBuilder.httpMethod
  override val sendApqExtensions: Boolean? get() = requestBuilder.sendApqExtensions
  override val sendDocument: Boolean? get() = requestBuilder.sendDocument
  override val enableAutoPersistedQueries: Boolean? get() = requestBuilder.enableAutoPersistedQueries
  override val canBeBatched: Boolean? get() = requestBuilder.canBeBatched
  override val httpHeaders: List<HttpHeader>? get() = requestBuilder.httpHeaders
  val ignoreApolloClientHttpHeaders: Boolean? get() = requestBuilder.ignoreApolloClientHttpHeaders
  @ApolloExperimental
  val retryOnError: Boolean? get() = requestBuilder.retryOnError
  @ApolloExperimental
  val failFastIfOffline: Boolean? get() = requestBuilder.failFastIfOffline

  fun failFastIfOffline(failFastIfOffline: Boolean?) = apply {
    requestBuilder.failFastIfOffline(failFastIfOffline)
  }

  override fun addExecutionContext(executionContext: ExecutionContext) = apply {
    requestBuilder.addExecutionContext(executionContext)
  }

  override fun httpMethod(httpMethod: HttpMethod?) = apply {
    requestBuilder.httpMethod(httpMethod)
  }

  override fun httpHeaders(httpHeaders: List<HttpHeader>?) = apply {
    requestBuilder.httpHeaders(httpHeaders)
  }

  override fun addHttpHeader(name: String, value: String) = apply {
    requestBuilder.addHttpHeader(name, value)
  }

  override fun sendApqExtensions(sendApqExtensions: Boolean?) = apply {
    requestBuilder.sendApqExtensions(sendApqExtensions)
  }

  override fun sendDocument(sendDocument: Boolean?) = apply {
    requestBuilder.sendDocument(sendDocument)
  }

  override fun enableAutoPersistedQueries(enableAutoPersistedQueries: Boolean?) = apply {
    requestBuilder.enableAutoPersistedQueries(enableAutoPersistedQueries)
  }

  override fun canBeBatched(canBeBatched: Boolean?) = apply {
    requestBuilder.canBeBatched(canBeBatched)
  }

  @ApolloExperimental
  fun retryOnError(retryOnError: Boolean?): ApolloCall<D> = apply {
    requestBuilder.retryOnError(retryOnError)
  }

  fun ignoreApolloClientHttpHeaders(ignoreApolloClientHttpHeaders: Boolean?) = apply {
    requestBuilder.ignoreApolloClientHttpHeaders(ignoreApolloClientHttpHeaders)
  }

  fun copy(): ApolloCall<D> {
    return ApolloCall(apolloClient, requestBuilder.build().newBuilder())
  }

  /**
   * Returns a cold Flow that produces [ApolloResponse]s from this [ApolloCall].
   *
   * The returned [Flow] does not throw on I/O errors or cache misses. Errors are not always terminal and some can be recovered. Check [ApolloResponse.exception] to handle errors:
   *
   * ```
   * apolloClient.subscription(NewOrders())
   *                  .toFlow()
   *                  .collect {
   *                    if (it.data != null) {
   *                      // Handle (potentially partial) data
   *                    } else {
   *                      // Something wrong happened
   *                      if (it.exception != null) {
   *                        // Handle fetch errors
   *                      } else {
   *                        // Handle GraphQL errors in response.errors
   *                      }
   *                    }
   *                  }
   * ```
   *
   * The returned [Flow] flows on the dispatcher configured in [ApolloClient.Builder.dispatcher] or a default dispatcher else. There is no need to change the coroutine context before calling [toFlow]. See [ApolloClient.Builder.dispatcher] for more details.
   *
   * The returned [Flow] has [kotlinx.coroutines.channels.Channel.UNLIMITED] buffering so that no response is missed in the case of a slow consumer. Use [kotlinx.coroutines.flow.buffer] to change that behaviour.
   *
   * @see toFlowV3
   * @see ApolloClient.Builder.dispatcher
   */
  fun toFlow(): Flow<ApolloResponse<D>> {
    return apolloClient.executeAsFlowInternal(requestBuilder.build(), false)
  }

  /**
   * A version of [execute] that restores 3.x behaviour:
   * - throw on fetch errors.
   * - make `CacheFirst`, `NetworkFirst` and `CacheAndNetwork` policies ignore fetch errors.
   * - throw ApolloComposite exception if needed.
   */
  @Deprecated("Use toFlow() and handle ApolloResponse.exception instead")
  @ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
  fun toFlowV3(): Flow<ApolloResponse<D>> {
    @Suppress("DEPRECATION")
    return conflateFetchPolicyInterceptorResponses(true)
        .apolloClient
        .executeAsFlowInternal(requestBuilder.build(), true)
  }

  /**
   * A version of [execute] that restores 3.x behaviour:
   * - throw on fetch errors.
   * - make `CacheFirst`, `NetworkFirst` and `CacheAndNetwork` policies ignore fetch errors.
   * - throw ApolloComposite exception if needed.
   */
  @Deprecated("Use execute() and handle ApolloResponse.exception instead")
  @ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
  suspend fun executeV3(): ApolloResponse<D> {
    @Suppress("DEPRECATION")
    return singleSuccessOrException(toFlowV3())
  }

  /**
   * Retrieves a single [ApolloResponse] from this [ApolloCall].
   *
   * [execute] calls [toFlow] and filters out cache or network errors to return a single success [ApolloResponse].
   *
   * [execute] throws if more than one success [ApolloResponse] is returned, for an example, if [operation] is a subscription or a `@defer` query.
   * In those cases use [toFlow] instead.
   *
   * [execute] may fail due to an I/O error, a cache miss or other reasons. In that case, check [ApolloResponse.exception]:
   * ```
   * val response = apolloClient.execute(ProductQuery())
   * if (response.data != null) {
   *   // Handle (potentially partial) data
   * } else {
   *   // Something wrong happened
   *   if (it.exception != null) {
   *     // Handle fetch errors
   *   } else {
   *     // Handle GraphQL errors in response.errors
   *   }
   * }
   * ```
   *
   * The work is executed on the dispatcher configured in [ApolloClient.Builder.dispatcher] or a default dispatcher else. There is no need to change the coroutine context before calling [execute]. See [ApolloClient.Builder.dispatcher] for more details.
   *
   * @throws ApolloException if the call returns zero or multiple valid GraphQL responses.
   *
   * @see executeV3
   * @see ApolloClient.Builder.dispatcher
   */
  suspend fun execute(): ApolloResponse<D> {
    return singleSuccessOrException(toFlow())
  }

  private suspend fun singleSuccessOrException(flow: Flow<ApolloResponse<D>>): ApolloResponse<D> {
    val responses = flow.toList()
    val (exceptionResponses, successResponses) = responses.partition { it.exception != null }
    return when (successResponses.size) {
      0 -> {
        when (exceptionResponses.size) {
          0 -> throw DefaultApolloException("The operation did not emit any item, check your interceptor chain")
          1 -> exceptionResponses.first()
          else -> {
            val first = exceptionResponses.first()
            first.newBuilder()
                .exception(
                    exceptionResponses.drop(1).fold(first.exception!!) { acc, response ->
                      acc.also {
                        it.addSuppressed(response.exception!!)
                      }
                    }
                )
                .build()
          }
        }
      }

      1 -> successResponses.first()
      else -> throw DefaultApolloException("The operation returned multiple items, use .toFlow() instead of .execute()")
    }
  }
}
