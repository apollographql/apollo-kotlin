package com.apollographql.apollo3

import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.cache.CacheHeaders
import com.apollographql.apollo3.cache.normalized.Record
import com.apollographql.apollo3.exception.ApolloCanceledException
import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.exception.ApolloHttpException
import com.apollographql.apollo3.exception.ApolloNetworkException
import com.apollographql.apollo3.exception.ApolloParseException
import com.apollographql.apollo3.fetcher.ResponseFetcher
import com.apollographql.apollo3.internal.util.Cancelable

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
   * Sets the [CacheHeaders] to use for this call. [com.apollographql.apollo3.interceptor.FetchOptions] will
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
   * [com.apollographql.apollo3.ApolloCall.Callback] will be disposed, and will receive no more events.
   * The call will attempt to abort and release resources, if possible.
   */
  override fun cancel()
  fun toBuilder(): Builder<D>
  interface Builder<D : Operation.Data> {
    fun build(): ApolloCall<D>

    /**
     * Sets the [CacheHeaders] to use for this call. [com.apollographql.apollo3.interceptor.FetchOptions] will
     * be configured with this headers, and will be accessible from the [ResponseFetcher] used for this call.
     *
     * @param cacheHeaders the [CacheHeaders] that will be passed with records generated from this request to [                     ]. Standardized cache headers are
     * defined in [ApolloCacheHeaders].
     * @return The builder
     */
    fun cacheHeaders(cacheHeaders: CacheHeaders): Builder<D>
  }

  /**
   * Communicates responses from a server, cached or offline requests.
   */
  abstract class Callback<D : Operation.Data> {
    /**
     * Gets called when GraphQL response is received and parsed successfully. Depending on the
     * [ResponseFetcher] used with the call, this may be called multiple times. [.onCompleted]
     * will be called after the final call to onResponse.
     *
     * @param response the GraphQL response
     */
    abstract fun onResponse(response: ApolloResponse<D>)

    /**
     * Gets called when the GraphQL response has been cached successfully.
     * It should be called only once from the cache.
     * This is used internally by watchers to retrieve the list of dependentKeys they have to watch.
     *
     * @param records the [List] of [Record] that was merged in cache.
     */
    open fun onCached(records: List<Record>) {}

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
     */
    open fun onHttpError(e: ApolloHttpException) {
      onFailure(e)
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