package com.apollographql.apollo3

import com.apollographql.apollo3.api.Mutation
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.cache.CacheHeaders
import com.apollographql.apollo3.fetcher.ResponseFetcher
import com.apollographql.apollo3.request.RequestHeaders

/**
 * A call prepared to execute GraphQL mutation operation.
 */
interface ApolloMutationCall<D : Operation.Data> : ApolloCall<D> {
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
  override fun cacheHeaders(cacheHeaders: CacheHeaders): ApolloMutationCall<D>

  /**
   *
   * Sets a list of [ApolloQueryWatcher] query names to be re-fetched once this mutation completed.
   *
   * @param operationNames array of query names to be re-fetched
   * @return [ApolloMutationCall] that will trigger re-fetching provided queries
   */
  @Deprecated("")
  fun refetchQueries(vararg operationNames: String): ApolloMutationCall<D>

  /**
   *
   * Sets a list of [Query] to be re-fetched once this mutation completed.
   *
   * @param queries array of [Query] to be re-fetched
   * @return [ApolloMutationCall] that will trigger re-fetching provided queries
   */
  @Deprecated("")
  fun refetchQueries(vararg queries: Query<*>): ApolloMutationCall<D>

  /**
   * Sets the [RequestHeaders] to use for this call. These headers will be added to the HTTP request when
   * it is issued. These headers will be applied after any headers applied by application-level interceptors
   * and will override those if necessary.
   *
   * @param requestHeaders The [RequestHeaders] to use for this request.
   * @return The ApolloCall object with the provided [RequestHeaders].
   */
  @Deprecated("")
  fun requestHeaders(requestHeaders: RequestHeaders): ApolloMutationCall<D>

  @Deprecated("")
  override fun clone(): ApolloMutationCall<D>

  override fun toBuilder(): Builder<D>
  interface Builder<D : Operation.Data> : ApolloCall.Builder<D> {
    override fun build(): ApolloMutationCall<D>
    override fun cacheHeaders(cacheHeaders: CacheHeaders): Builder<D>

    /**
     *
     * Sets a list of [ApolloQueryWatcher] query names to be re-fetched once this mutation completed.
     *
     * @param operationNames array of query names to be re-fetched
     * @return The Builder
     */
    fun refetchQueryNames(operationNames: List<String>): Builder<D>

    /**
     *
     * Sets a list of [Query] to be re-fetched once this mutation completed.
     *
     * @param queries array of [Query] to be re-fetched
     * @return The Builder
     */
    fun refetchQueries(queries: List<Query<*>>): Builder<D>

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
   * Factory for creating [ApolloMutationCall] calls.
   */
  interface Factory {
    /**
     * Creates and prepares a new [ApolloMutationCall] call.
     *
     * @param mutation the [Mutation] which needs to be performed
     * @return prepared [ApolloMutationCall] call to be executed at some point in the future
     */
    fun <D : Mutation.Data> mutate(
        mutation: Mutation<D>): ApolloMutationCall<D>

    /**
     *
     * Creates and prepares a new [ApolloMutationCall] call with optimistic updates.
     *
     * Provided optimistic updates will be stored in [com.apollographql.apollo3.cache.normalized.ApolloStore]
     * immediately before mutation execution. Any [ApolloQueryWatcher] dependent on the changed cache records will
     * be re-fetched.
     *
     * @param mutation              the [Mutation] which needs to be performed
     * @param withOptimisticUpdates optimistic updates for this mutation
     * @return prepared [ApolloMutationCall] call to be executed at some point in the future
     */
    fun <D : Mutation.Data> mutate(
        mutation: Mutation<D>, withOptimisticUpdates: D): ApolloMutationCall<D>
  }
}
