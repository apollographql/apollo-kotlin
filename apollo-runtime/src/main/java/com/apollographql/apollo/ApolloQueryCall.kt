package com.apollographql.apollo;

import com.apollographql.apollo.api.Operation;
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
public interface ApolloQueryCall<D extends Operation.Data> extends ApolloCall<D> {
  /**
   * Returns a watcher to watch the changes to the normalized cache records this query depends on or when mutation call
   * triggers to re-fetch this query after it completes via {@link ApolloMutationCall#refetchQueries(OperationName...)}
   *
   * @return {@link ApolloQueryWatcher}
   */
  @NotNull ApolloQueryWatcher<D> watcher();

  /**
   * Sets the http cache policy for response/request cache.
   *
   * Deprecated, use {@link #toBuilder()} to mutate the ApolloCall
   *
   * @param httpCachePolicy {@link HttpCachePolicy.Policy} to set
   * @return {@link ApolloQueryCall} with the provided {@link HttpCachePolicy.Policy}
   */
  @Deprecated @NotNull ApolloQueryCall<D> httpCachePolicy(@NotNull HttpCachePolicy.Policy httpCachePolicy);

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
  @Deprecated @NotNull @Override ApolloQueryCall<D> cacheHeaders(@NotNull CacheHeaders cacheHeaders);

  /**
   * Sets the {@link ResponseFetcher} strategy for an ApolloCall object.
   *
   * Deprecated, use {@link #toBuilder()} to mutate the ApolloCall
   *
   * @param fetcher the {@link ResponseFetcher} to use.
   * @return The ApolloCall object with the provided CacheControl strategy
   */
  @Deprecated @NotNull ApolloQueryCall<D> responseFetcher(@NotNull ResponseFetcher fetcher);

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
  @Deprecated @NotNull ApolloQueryCall<D> requestHeaders(@NotNull RequestHeaders requestHeaders);

  @Deprecated @NotNull @Override ApolloQueryCall<D> clone();

  @NotNull @Override Builder<D> toBuilder();

  interface Builder<D extends Operation.Data> extends ApolloCall.Builder<D> {
    @NotNull @Override ApolloQueryCall<D> build();

    /**
     * Sets the {@link CacheHeaders} to use for this call. {@link com.apollographql.apollo.interceptor.FetchOptions} will
     * be configured with this headers, and will be accessible from the {@link ResponseFetcher} used for this call.
     *
     * @param cacheHeaders the {@link CacheHeaders} that will be passed with records generated from this request to {@link
     *                     com.apollographql.apollo.cache.normalized.NormalizedCache}. Standardized cache headers are
     *                     defined in {@link com.apollographql.apollo.cache.ApolloCacheHeaders}.
     * @return The ApolloCall object with the provided {@link CacheHeaders}.
     */
    @NotNull @Override Builder<D> cacheHeaders(@NotNull CacheHeaders cacheHeaders);

    /**
     * Sets the http cache policy for response/request cache.
     *
     * @param httpCachePolicy {@link HttpCachePolicy.Policy} to set
     * @return {@link ApolloQueryCall} with the provided {@link HttpCachePolicy.Policy}
     */
    @NotNull Builder<D> httpCachePolicy(@NotNull HttpCachePolicy.Policy httpCachePolicy);

    /**
     * Sets the {@link ResponseFetcher} strategy for an ApolloCall object.
     *
     * @param fetcher the {@link ResponseFetcher} to use.
     * @return The ApolloCall object with the provided CacheControl strategy
     */
    @NotNull Builder<D> responseFetcher(@NotNull ResponseFetcher fetcher);

    /**
     * Sets the {@link RequestHeaders} to use for this call. These headers will be added to the HTTP request when
     * it is issued. These headers will be applied after any headers applied by application-level interceptors
     * and will override those if necessary.
     *
     * @param requestHeaders The {@link RequestHeaders} to use for this request.
     * @return The Builder
     */
    @NotNull Builder<D> requestHeaders(@NotNull RequestHeaders requestHeaders);
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
    <D extends Query.Data> ApolloQueryCall<D> query(@NotNull Query<D> query);
  }
}
