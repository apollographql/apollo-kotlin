package com.apollographql.android;

import com.apollographql.android.api.graphql.Operation;
import com.apollographql.android.cache.normalized.CacheControl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface ApolloWatcher<T extends Operation.Data> {

  interface WatcherSubscription {
    void unsubscribe();
  }

  @Nonnull WatcherSubscription enqueueAndWatch(@Nullable final ApolloCall.Callback<T> callback);

  @Nonnull ApolloWatcher<T> refetchCacheControl(@Nonnull CacheControl cacheControl);

  @Nonnull ApolloWatcher<T> refetch();
}
