package com.apollographql.apollo;

import com.apollographql.apollo.internal.util.Cancelable;
import com.apollographql.apollo.cache.normalized.CacheControl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface ApolloQueryWatcher<T> extends Cancelable {

  void enqueueAndWatch(@Nullable final ApolloCall.Callback<T> callback);

  /**
   * @param cacheControl The {@link CacheControl} to use when the call is refetched due to a field changing in the
   *                     cache.
   */
  @Nonnull ApolloQueryWatcher<T> refetchCacheControl(@Nonnull CacheControl cacheControl);

}
