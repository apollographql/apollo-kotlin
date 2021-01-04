package com.apollographql.apollo

import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.cache.CacheHeaders
import com.apollographql.apollo.exception.ApolloCanceledException
import com.apollographql.apollo.exception.ApolloException
import com.apollographql.apollo.exception.ApolloHttpException
import com.apollographql.apollo.exception.ApolloNetworkException
import com.apollographql.apollo.exception.ApolloParseException
import com.apollographql.apollo.fetcher.ResponseFetcher
import com.apollographql.apollo.internal.util.Cancelable

/**
 *
 * ApolloCall is an abstraction for a request that has been prepared for execution. ApolloCall represents a single
 * request/response pair and cannot be executed twice, though it can be cancelled.
 *
 *
 * In order to execute the request again, call the [ApolloCall.clone] method which creates a new ApolloCall
 * object.
 */
interface ApolloCall<D : Operation.Data> : Cancelable {
  /**
   * Schedules the request to be executed at some point in the future.
   *
   * @param callback Callback which will handle the response or a failure exception.
   * @throws IllegalStateException when the call has already been executed
   */
  fun enqueue(callback: Callback<D>?)

  /**
   * Sets the [CacheHeaders] to use for this call. [com.apollographql.apollo.interceptor.FetchOptions] will
   * be configured with this headers, and will be accessible from the [ResponseFetcher] used for this call.
   *
   * Deprecated, use [.toBuilder] to mutate the ApolloCall
   *
   * @param cacheHeaders the [CacheHeaders] that will be passed with records generated from this request to [                     ]. Standardized cache headers are
   * defined in [ApolloCacheHeaders].
   * @return The ApolloCall object with the provided [CacheHeaders].
   */
  @Deprecated("")
  fun cacheHeaders(cacheHeaders: CacheHeaders): ApolloCall<D>

  /**
   * Creates a new, identical call to this one which can be enqueued or executed even if this call has already been.
   *
   * Deprecated, use [.toBuilder] to mutate the ApolloCall
   *
   * @return The cloned ApolloCall object.
   */
  @Deprecated("")
  fun clone(): ApolloCall<D>

  /**
   * Returns GraphQL operation this call executes
   *
   * @return [Operation]
   */
  fun operation(): Operation<D>

  /**
   * Cancels this [ApolloCall]. If the call was started with [.enqueue], the
   * [com.apollographql.apollo.ApolloCall.Callback] will be disposed, and will receive no more events.
   * The call will attempt to abort and release resources, if possible.
   */
  override fun cancel()
  fun toBuilder(): Builder<D>
  interface Builder<D : Operation.Data> {
    fun build(): ApolloCall<D>

    /**
     * Sets the [CacheHeaders] to use for this call. [com.apollographql.apollo.interceptor.FetchOptions] will
     * be configured with this headers, and will be accessible from the [ResponseFetcher] used for this call.
     *
     * @param cacheHeaders the [CacheHeaders] that will be passed with records generated from this request to [                     ]. Standardized cache headers are
     * defined in [ApolloCacheHeaders].
     * @return The builder
     */
    fun cacheHeaders(cacheHeaders: CacheHeaders): Builder<D>
  }

  /**
   * Communicates responses from a server or offline requests.
   */
  abstract class Callback<D : Operation.Data> {
    /**
     * Gets called when GraphQL response is received and parsed successfully. Depending on the
     * [ResponseFetcher] used with the call, this may be called multiple times. [.onCompleted]
     * will be called after the final call to onResponse.
     *
     * @param response the GraphQL response
     */
    abstract fun onResponse(response: Response<D>)

    /**
     * Gets called when an unexpected exception occurs while creating the request or processing the response.
     * Will be called at most one time. It is considered a terminal event. After called,
     * neither [.onResponse] or [.onCompleted] will be called again.
     */
    abstract fun onFailure(e: ApolloException)

    /**
     * Gets called whenever any action happen to this [ApolloCall].
     *
     * @param event status that corresponds to a [ApolloCall] action
     */
    open fun onStatusEvent(event: StatusEvent) {}

    /**
     *
     * Gets called when an http request error takes place. This is the case when the returned http status code
     * doesn't lie in the range 200 (inclusive) and 300 (exclusive).
     *
     * **NOTE:** by overriding this callback you must call [okhttp3.Response.close] on [ ][ApolloHttpException.rawResponse] to close the network connection.
     */
    open fun onHttpError(e: ApolloHttpException) {
      onFailure(e)
      val response = e.rawResponse()
      response?.close()
    }

    /**
     * Gets called when an http request error takes place due to network failures, timeouts etc.
     */
    open fun onNetworkError(e: ApolloNetworkException) {
      onFailure(e)
    }

    /**
     * Gets called when the network request succeeds but there was an error parsing the response.
     */
    open fun onParseError(e: ApolloParseException) {
      onFailure(e)
    }

    /**
     * Gets called when [ApolloCall] has been canceled.
     */
    open fun onCanceledError(e: ApolloCanceledException) {
      onFailure(e)
    }
  }

  /**
   * Represents a status event that corresponds to a [ApolloCall] action
   */
  enum class StatusEvent {
    /**
     * [ApolloCall] is scheduled for execution
     */
    SCHEDULED,

    /**
     * [ApolloCall] fetches response from cache
     */
    FETCH_CACHE,

    /**
     * [ApolloCall] fetches response from network
     */
    FETCH_NETWORK,

    /**
     * [ApolloCall] is finished its execution
     */
    COMPLETED
  }
}