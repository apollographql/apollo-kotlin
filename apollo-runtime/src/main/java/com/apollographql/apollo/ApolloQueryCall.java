package com.apollographql.apollo;

import com.apollographql.apollo.api.OperationName;
import com.apollographql.apollo.api.Query;
import com.apollographql.apollo.cache.CacheHeaders;
import com.apollographql.apollo.cache.http.HttpCachePolicy;
import com.apollographql.apollo.cache.normalized.CacheControl;

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

  @Nonnull @Override ApolloQueryCall<T> cacheControl(@Nonnull CacheControl cacheControl);

  @Nonnull @Override ApolloQueryCall<T> cacheHeaders(@Nonnull CacheHeaders cacheHeaders);

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
