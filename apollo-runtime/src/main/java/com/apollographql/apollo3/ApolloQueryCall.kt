package com.apollographql.apollo3

import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.api.cache.http.HttpCachePolicy
import com.apollographql.apollo3.cache.CacheHeaders
import com.apollographql.apollo3.fetcher.ResponseFetcher
import com.apollographql.apollo3.request.RequestHeaders

/**
 * A call prepared to execute GraphQL query operation.
 */
interface ApolloQueryCall<D : Operation.Data> : ApolloCall<D> {
  /**
   * Returns a watcher to watch the changes to the normalized cache records this query depends on or when mutation call
   * triggers to re-fetch this query after it completes via [ApolloMutationCall.refetchQueries]
   *
   * @return [ApolloQueryWatcher]
   */
  fun watcher(): ApolloQueryWatcher<D>

  /**
   * Sets the http cache policy for response/request cache.
   *
   * Deprecated, use [.toBuilder] to mutate the ApolloCall
   *
   * @param httpCachePolicy [HttpCachePolicy.Policy] to set
   * @return [ApolloQueryCall] with the provided [HttpCachePolicy.Policy]
   */
  @Deprecated("")
  fun httpCachePolicy(httpCachePolicy: HttpCachePolicy.Policy): ApolloQueryCall<D>

  /**
   * Sets the [CacheHeaders] to use for this call. [com.apollographql.apollo3.interceptor.FetchOptions] will
   * be configured with this headers, and will be accessible from the [ResponseFetcher] used for this call.
   *
   * Deprecated, use [.toBuilder] to mutate the ApolloCall
   *
   * @param cacheHeaders the [CacheHeaders] that will be passed with records generated from this request to [                     ]. Standardized cache headers are
   * defined in [com.apollographql.apollo3.cache.ApolloCacheHeaders].
   * @return The ApolloCall object with the provided [CacheHeaders].
   */
  @Deprecated("")
  override fun cacheHeaders(cacheHeaders: CacheHeaders): ApolloQueryCall<D>

  /**
   * Sets the [ResponseFetcher] strategy for an ApolloCall object.
   *
   * Deprecated, use [.toBuilder] to mutate the ApolloCall
   *
   * @param fetcher the [ResponseFetcher] to use.
   * @return The ApolloCall object with the provided CacheControl strategy
   */
  @Deprecated("")
  fun responseFetcher(fetcher: ResponseFetcher): ApolloQueryCall<D>

  /**
   * Sets the [RequestHeaders] to use for this call. These headers will be added to the HTTP request when
   * it is issued. These headers will be applied after any headers applied by application-level interceptors
   * and will override those if necessary.
   *
   * Deprecated, use [.toBuilder] to mutate the ApolloCall
   *
   * @param requestHeaders The [RequestHeaders] to use for this request.
   * @return The ApolloCall object with the provided [RequestHeaders].
   */
  @Deprecated("")
  fun requestHeaders(requestHeaders: RequestHeaders): ApolloQueryCall<D>
  @Deprecated("")
  override fun clone(): ApolloQueryCall<D>
  override fun toBuilder(): Builder<D>
  interface Builder<D : Operation.Data> : ApolloCall.Builder<D> {
    override fun build(): ApolloQueryCall<D>

    /**
     * Sets the [CacheHeaders] to use for this call. [com.apollographql.apollo3.interceptor.FetchOptions] will
     * be configured with this headers, and will be accessible from the [ResponseFetcher] used for this call.
     *
     * @param cacheHeaders the [CacheHeaders] that will be passed with records generated from this request to [                     ]. Standardized cache headers are
     * defined in [com.apollographql.apollo3.cache.ApolloCacheHeaders].
     * @return The ApolloCall object with the provided [CacheHeaders].
     */
    override fun cacheHeaders(cacheHeaders: CacheHeaders): Builder<D>

    /**
     * Sets the http cache policy for response/request cache.
     *
     * @param httpCachePolicy [HttpCachePolicy.Policy] to set
     * @return [ApolloQueryCall] with the provided [HttpCachePolicy.Policy]
     */
    fun httpCachePolicy(httpCachePolicy: HttpCachePolicy.Policy): Builder<D>

    /**
     * Sets the [ResponseFetcher] strategy for an ApolloCall object.
     *
     * @param fetcher the [ResponseFetcher] to use.
     * @return The ApolloCall object with the provided CacheControl strategy
     */
    fun responseFetcher(fetcher: ResponseFetcher): Builder<D>

    /**
     * Sets the [RequestHeaders] to use for this call. These headers will be added to the HTTP request when
     * it is issued. These headers will be applied after any headers applied by application-level interceptors
     * and will override those if necessary.
     *
     * @param requestHeaders The [RequestHeaders] to use for this request.
     * @return The Builder
     */
    fun requestHeaders(requestHeaders: RequestHeaders): Builder<D>
  }

  /**
   * Factory for creating [ApolloQueryCall] calls.
   */
  interface Factory {
    /**
     * Creates and prepares a new [ApolloQueryCall] call.
     *
     * @param query the operation which needs to be performed
     * @return prepared [ApolloQueryCall] call to be executed at some point in the future
     */
    fun <D : Query.Data> query(query: Query<D>): ApolloQueryCall<D>
  }
}