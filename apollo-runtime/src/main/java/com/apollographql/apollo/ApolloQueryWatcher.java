package com.apollographql.apollo;

import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.fetcher.ResponseFetcher;
import com.apollographql.apollo.internal.util.Cancelable;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ApolloQueryWatcher<T> extends Cancelable {

  ApolloQueryWatcher<T> enqueueAndWatch(@Nullable final ApolloCall.Callback<T> callback);

  /**
   * @param fetcher The {@link ResponseFetcher} to use when the call is refetched due to a field changing in the
   *                     cache.
   */
  @NotNull ApolloQueryWatcher<T> refetchResponseFetcher(@NotNull ResponseFetcher fetcher);

  /**
   * Returns GraphQL watched operation.
   *
   * @return {@link Operation}
   */
  @NotNull Operation operation();

  /**
   * Re-fetches watched GraphQL query.
   */
  void refetch();

  /**
   * Cancels this {@link ApolloQueryWatcher}. The {@link com.apollographql.apollo.ApolloCall.Callback}
   * will be disposed, and will receive no more events. Any active operations will attempt to abort and
   * release resources, if possible.
   */
  @Override void cancel();

}
