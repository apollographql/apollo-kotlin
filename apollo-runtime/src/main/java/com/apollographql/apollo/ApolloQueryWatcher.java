package com.apollographql.apollo;

import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.fetcher.ResponseFetcher;
import com.apollographql.apollo.internal.util.Cancelable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface ApolloQueryWatcher<T> extends Cancelable {

  ApolloQueryWatcher<T> enqueueAndWatch(@Nullable final ApolloCall.Callback<T> callback);

  /**
   * @param fetcher The {@link ResponseFetcher} to use when the call is refetched due to a field changing in the
   *                     cache.
   */
  @Nonnull ApolloQueryWatcher<T> refetchResponseFetcher(@Nonnull ResponseFetcher fetcher);

  /**
   * Returns GraphQL watched operation.
   *
   * @return {@link Operation}
   */
  @Nonnull Operation operation();

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
