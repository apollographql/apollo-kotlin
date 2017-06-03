package com.apollographql.apollo;

import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.cache.normalized.CacheControl;
import com.apollographql.apollo.internal.util.Cancelable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface ApolloQueryWatcher<T> extends Cancelable {

  ApolloQueryWatcher<T> enqueueAndWatch(@Nullable final ApolloCall.Callback<T> callback);

  /**
   * @param cacheControl The {@link CacheControl} to use when the call is refetched due to a field changing in the
   *                     cache.
   */
  @Nonnull ApolloQueryWatcher<T> refetchCacheControl(@Nonnull CacheControl cacheControl);

  /**
   * Returns GraphQl watched operation.
   *
   * @return {@link Operation}
   */
  @Nonnull Operation operation();

  /**
   * Re-fetches watched GraphQl query.
   */
  void refetch();
}
