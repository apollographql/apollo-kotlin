package com.apollographql.apollo3

import com.apollographql.apollo3.annotations.ApolloDeprecatedSince
import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.ExecutionContext
import com.apollographql.apollo3.api.MutableExecutionOptions
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.http.HttpHeader
import com.apollographql.apollo3.api.http.HttpMethod
import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.exception.DefaultApolloException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList

/**
 * An [ApolloCall] is a thin class that binds an [ApolloRequest] with its [ApolloClient].
 *
 * - call [ApolloCall.execute] for simple request/response cases.
 * - call [ApolloCall.toFlow] for other cases that may return more than one [ApolloResponse]. For an example
 * subscriptions, `@defer` queries, cache queries, etc...
 */
class ApolloCall<D : Operation.Data> internal constructor(
    internal val apolloClient: ApolloClient,
    val operation: Operation<D>,
) : MutableExecutionOptions<ApolloCall<D>> {
  override var executionContext: ExecutionContext = ExecutionContext.Empty
    private set
  override var httpMethod: HttpMethod? = null
    private set
  override var sendApqExtensions: Boolean? = null
    private set
  override var sendDocument: Boolean? = null
    private set
  override var enableAutoPersistedQueries: Boolean? = null
    private set
  override var canBeBatched: Boolean? = null
    private set

  @ApolloExperimental
  var retryOnError: Boolean? = null
    private set

  @ApolloExperimental
  var failFastIfOffline: Boolean? = null
    private set

  /**
   * The HTTP headers to be sent with the request.
   * By default, these are *added* on top of any HTTP header previously set on [ApolloClient]. Call [ignoreApolloClientHttpHeaders]`(true)`
   * to instead *ignore* the ones set on [ApolloClient].
   */
  override var httpHeaders: List<HttpHeader>? = null
    private set

  var ignoreApolloClientHttpHeaders: Boolean? = null
    private set

  fun failFastIfOffline(failFastIfOffline: Boolean?) = apply {
    this.failFastIfOffline = failFastIfOffline
  }

  override fun addExecutionContext(executionContext: ExecutionContext) = apply {
    this.executionContext = this.executionContext + executionContext
  }

  override fun httpMethod(httpMethod: HttpMethod?) = apply {
    this.httpMethod = httpMethod
  }

  /**
   * Sets the HTTP headers to be sent with the request.
   * By default, these are *added* on top of any HTTP header previously set on [ApolloClient]. Call [ignoreApolloClientHttpHeaders]`(true)`
   * to instead *ignore* the ones set on [ApolloClient].
   */
  override fun httpHeaders(httpHeaders: List<HttpHeader>?) = apply {
    this.httpHeaders = httpHeaders
  }

  /**
   * Add an HTTP header to be sent with the request.
   * By default, these are *added* on top of any HTTP header previously set on [ApolloClient]. Call [ignoreApolloClientHttpHeaders]`(true)`
   * to instead *ignore* the ones set on [ApolloClient].
   */
  override fun addHttpHeader(name: String, value: String) = apply {
    this.httpHeaders = (this.httpHeaders ?: emptyList()) + HttpHeader(name, value)
  }

  override fun sendApqExtensions(sendApqExtensions: Boolean?) = apply {
    this.sendApqExtensions = sendApqExtensions
  }

  override fun sendDocument(sendDocument: Boolean?) = apply {
    this.sendDocument = sendDocument
  }

  override fun enableAutoPersistedQueries(enableAutoPersistedQueries: Boolean?) = apply {
    this.enableAutoPersistedQueries = enableAutoPersistedQueries
  }

  override fun canBeBatched(canBeBatched: Boolean?) = apply {
    this.canBeBatched = canBeBatched
  }

  @ApolloExperimental
  fun retryOnError(retryOnError: Boolean?): ApolloCall<D> = apply {
    this.retryOnError = retryOnError
  }

  /**
   * If set to true, the HTTP headers set on [ApolloClient] will not be used for the call, only the ones set on this [ApolloCall] will be
   * used. If set to false, both sets of headers will be concatenated and used.
   *
   * Default: false
   */
  fun ignoreApolloClientHttpHeaders(ignoreApolloClientHttpHeaders: Boolean?) = apply {
    this.ignoreApolloClientHttpHeaders = ignoreApolloClientHttpHeaders
  }

  fun copy(): ApolloCall<D> {
    return ApolloCall(apolloClient, operation)
        .addExecutionContext(executionContext)
        .httpMethod(httpMethod)
        .httpHeaders(httpHeaders)
        .ignoreApolloClientHttpHeaders(ignoreApolloClientHttpHeaders)
        .sendApqExtensions(sendApqExtensions)
        .sendDocument(sendDocument)
        .enableAutoPersistedQueries(enableAutoPersistedQueries)
        .canBeBatched(canBeBatched)
        .retryOnError(retryOnError)
        .failFastIfOffline(failFastIfOffline)
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
   *                        // Handle non-GraphQL errors
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
    return apolloClient.executeAsFlow(toApolloRequest(), ignoreApolloClientHttpHeaders = ignoreApolloClientHttpHeaders == true, false)
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
        .executeAsFlow(toApolloRequest(), ignoreApolloClientHttpHeaders = ignoreApolloClientHttpHeaders == true, true)
  }

  private fun toApolloRequest(): ApolloRequest<D> {
    return ApolloRequest.Builder(operation)
        .executionContext(executionContext)
        .httpMethod(httpMethod)
        .httpHeaders(httpHeaders)
        .sendApqExtensions(sendApqExtensions)
        .sendDocument(sendDocument)
        .enableAutoPersistedQueries(enableAutoPersistedQueries)
        .canBeBatched(canBeBatched)
        .retryOnError(retryOnError)
        .failFastIfOffline(failFastIfOffline)
        .build()
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
   * Use this for queries and mutations to get a single value from the network or the cache.
   * For subscriptions or operations using `@defer` that may return multiple values, use [toFlow] instead.
   *
   * [execute] may fail due to an I/O error, a cache miss or other reasons. In that case, check [ApolloResponse.exception]:
   *
   * ```
   * val response = apolloClient.execute(ProductQuery())
   * if (response.data != null) {
   *   // Handle (potentially partial) data
   * } else {
   *   // Something wrong happened
   *   if (it.exception != null) {
   *     // Handle non-GraphQL errors
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
