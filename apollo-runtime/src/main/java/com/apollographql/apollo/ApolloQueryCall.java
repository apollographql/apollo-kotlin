package com.apollographql.apollo;

import com.apollographql.apollo.api.OperationName;
import com.apollographql.apollo.api.Query;
import com.apollographql.apollo.api.cache.http.HttpCachePolicy;
import com.apollographql.apollo.cache.CacheHeaders;
import com.apollographql.apollo.fetcher.ResponseFetcher;
import com.apollographql.apollo.request.RequestHeaders;
import org.jetbrains.annotations.NotNull;

/**
 * A call prepared to execute GraphQL query operation.
 */
public interface ApolloQueryCall<T> extends ApolloCall<T> {
  /**
   * Returns a watcher to watch the changes to the normalized cache records this query depends on or when mutation call
   * triggers to re-fetch this query after it completes via {@link ApolloMutationCall#refetchQueries(OperationName...)}
   *
   * @return {@link ApolloQueryWatcher}
   */
  @NotNull ApolloQueryWatcher<T> watcher();

  /**
   * Sets the http cache policy for response/request cache.
   *
   * Deprecated, use {@link #toBuilder()} to mutate the ApolloCall
   *
   * @param httpCachePolicy {@link HttpCachePolicy.Policy} to set
   * @return {@link ApolloQueryCall} with the provided {@link HttpCachePolicy.Policy}
   */
  @Deprecated @NotNull ApolloQueryCall<T> httpCachePolicy(@NotNull HttpCachePolicy.Policy httpCachePolicy);

  /**
   * Sets the {@link CacheHeaders} to use for this call. {@link com.apollographql.apollo.interceptor.FetchOptions} will
   * be configured with this headers, and will be accessible from the {@link ResponseFetcher} used for this call.
   *
   * Deprecated, use {@link #toBuilder()} to mutate the ApolloCall
   *
   * @param cacheHeaders the {@link CacheHeaders} that will be passed with records generated from this request to {@link
   *                     com.apollographql.apollo.cache.normalized.NormalizedCache}. Standardized cache headers are
   *                     defined in {@link com.apollographql.apollo.cache.ApolloCacheHeaders}.
   * @return The ApolloCall object with the provided {@link CacheHeaders}.
   */
  @Deprecated @NotNull @Override ApolloQueryCall<T> cacheHeaders(@NotNull CacheHeaders cacheHeaders);

  /**
   * Sets the {@link ResponseFetcher} strategy for an ApolloCall object.
   *
   * Deprecated, use {@link #toBuilder()} to mutate the ApolloCall
   *
   * @param fetcher the {@link ResponseFetcher} to use.
   * @return The ApolloCall object with the provided CacheControl strategy
   */
  @Deprecated @NotNull ApolloQueryCall<T> responseFetcher(@NotNull ResponseFetcher fetcher);

  /**
   * Sets the {@link RequestHeaders} to use for this call. These headers will be added to the HTTP request when
   * it is issued. These headers will be applied after any headers applied by application-level interceptors
   * and will override those if necessary.
   *
   * Deprecated, use {@link #toBuilder()} to mutate the ApolloCall
   *
   * @param requestHeaders The {@link RequestHeaders} to use for this request.
   * @return The ApolloCall object with the provided {@link RequestHeaders}.
   */
  @Deprecated @NotNull ApolloQueryCall<T> requestHeaders(@NotNull RequestHeaders requestHeaders);

  @Deprecated @NotNull @Override ApolloQueryCall<T> clone();

  @NotNull @Override Builder<T> toBuilder();

  interface Builder<T> extends ApolloCall.Builder<T> {
    @NotNull @Override ApolloQueryCall<T> build();

    /**
     * Sets the {@link CacheHeaders} to use for this call. {@link com.apollographql.apollo.interceptor.FetchOptions} will
     * be configured with this headers, and will be accessible from the {@link ResponseFetcher} used for this call.
     *
     * @param cacheHeaders the {@link CacheHeaders} that will be passed with records generated from this request to {@link
     *                     com.apollographql.apollo.cache.normalized.NormalizedCache}. Standardized cache headers are
     *                     defined in {@link com.apollographql.apollo.cache.ApolloCacheHeaders}.
     * @return The ApolloCall object with the provided {@link CacheHeaders}.
     */
    @NotNull @Override Builder<T> cacheHeaders(@NotNull CacheHeaders cacheHeaders);

    /**
     * Sets the http cache policy for response/request cache.
     *
     * @param httpCachePolicy {@link HttpCachePolicy.Policy} to set
     * @return {@link ApolloQueryCall} with the provided {@link HttpCachePolicy.Policy}
     */
    @NotNull Builder<T> httpCachePolicy(@NotNull HttpCachePolicy.Policy httpCachePolicy);

    /**
     * Sets the {@link ResponseFetcher} strategy for an ApolloCall object.
     *
     * @param fetcher the {@link ResponseFetcher} to use.
     * @return The ApolloCall object with the provided CacheControl strategy
     */
    @NotNull Builder<T> responseFetcher(@NotNull ResponseFetcher fetcher);

    /**
     * Sets the {@link RequestHeaders} to use for this call. These headers will be added to the HTTP request when
     * it is issued. These headers will be applied after any headers applied by application-level interceptors
     * and will override those if necessary.
     *
     * @param requestHeaders The {@link RequestHeaders} to use for this request.
     * @return The Builder
     */
    @NotNull Builder<T> requestHeaders(@NotNull RequestHeaders requestHeaders);

    /**
     * Allows this query to be part of a batch HTTP call to improve performance
     *
     * @param canBeBatched whether this query can be batched with others or not
     * @return The Builder
     */
    @NotNull Builder<T> canBeBatched(boolean canBeBatched);
  }

  /**
   * Factory for creating {@link ApolloQueryCall} calls.
   */
  interface Factory {
    /**
     * Creates and prepares a new {@link ApolloQueryCall} call.
     *
     * @param query the operation which needs to be performed
     * @return prepared {@link ApolloQueryCall} call to be executed at some point in the future
     */
    <D extends Query.Data, T, V extends Query.Variables> ApolloQueryCall<T> query(@NotNull Query<D, T, V> query);
  }
}
