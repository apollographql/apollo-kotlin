package com.apollographql.apollo;

import com.apollographql.apollo.api.OperationName;
import com.apollographql.apollo.api.Query;
import com.apollographql.apollo.cache.CacheHeaders;
import com.apollographql.apollo.api.cache.http.HttpCachePolicy;
import com.apollographql.apollo.fetcher.ResponseFetcher;

import javax.annotation.Nonnull;

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
  @Nonnull ApolloQueryWatcher<T> watcher();

  /**
   * Sets the http cache policy for response/request cache.
   *
   * @param httpCachePolicy {@link HttpCachePolicy.Policy} to set
   * @return {@link ApolloQueryCall} with the provided {@link HttpCachePolicy.Policy}
   */
  @Nonnull ApolloQueryCall<T> httpCachePolicy(@Nonnull HttpCachePolicy.Policy httpCachePolicy);

  /**
   * Sets the {@link CacheHeaders} to use for this call. {@link com.apollographql.apollo.interceptor.FetchOptions} will
   * be configured with this headers, and will be accessible from the {@link ResponseFetcher} used for this call.
   *
   * @param cacheHeaders the {@link CacheHeaders} that will be passed with records generated from this request to {@link
   *                     com.apollographql.apollo.cache.normalized.NormalizedCache}. Standardized cache headers are
   *                     defined in {@link com.apollographql.apollo.cache.ApolloCacheHeaders}.
   * @return The ApolloCall object with the provided {@link CacheHeaders}.
   */
  @Nonnull @Override ApolloQueryCall<T> cacheHeaders(@Nonnull CacheHeaders cacheHeaders);

  /**
   * Sets the {@link ResponseFetcher} strategy for an ApolloCall object.
   *
   * @param fetcher the {@link ResponseFetcher} to use.
   * @return The ApolloCall object with the provided CacheControl strategy
   */
  @Nonnull ApolloQueryCall<T> responseFetcher(@Nonnull ResponseFetcher fetcher);

  @Nonnull @Override ApolloQueryCall<T> clone();

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
    <D extends Query.Data, T, V extends Query.Variables> ApolloQueryCall<T> query(@Nonnull Query<D, T, V> query);
  }
}
