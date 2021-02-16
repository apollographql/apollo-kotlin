package com.apollographql.apollo3

import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.fetcher.ResponseFetcher
import com.apollographql.apollo3.internal.util.Cancelable

interface ApolloQueryWatcher<D : Operation.Data> : Cancelable {
  fun enqueueAndWatch(callback: ApolloCall.Callback<D>): ApolloQueryWatcher<D>

  /**
   * @param fetcher The [ResponseFetcher] to use when the call is refetched due to a field changing in the
   * cache.
   */
  fun refetchResponseFetcher(fetcher: ResponseFetcher): ApolloQueryWatcher<D>

  /**
   * Returns GraphQL watched operation.
   *
   * @return [Operation]
   */
  fun operation(): Operation<*>

  /**
   * Re-fetches watched GraphQL query.
   */
  fun refetch()

  /**
   * Cancels this [ApolloQueryWatcher]. The [com.apollographql.apollo3.ApolloCall.Callback]
   * will be disposed, and will receive no more events. Any active operations will attempt to abort and
   * release resources, if possible.
   */
  override fun cancel()

  /**
   * Creates a new, identical call to this one which can be enqueued or executed even if this call has already been.
   *
   * @return The cloned ApolloQueryWatcher object.
   */
  fun clone(): ApolloQueryWatcher<D>
}