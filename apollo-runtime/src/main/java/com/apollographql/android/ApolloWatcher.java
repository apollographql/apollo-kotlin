package com.apollographql.android;

import com.apollographql.android.api.graphql.Operation;
import com.apollographql.android.cache.normalized.CacheControl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface ApolloWatcher<T extends Operation.Data> {

  @Nonnull void enqueueAndWatch(@Nullable final ApolloCall.Callback<T> callback);

  @Nonnull ApolloWatcher<T> refetchCacheControl(@Nonnull CacheControl cacheControl);

  void cancel();

}
