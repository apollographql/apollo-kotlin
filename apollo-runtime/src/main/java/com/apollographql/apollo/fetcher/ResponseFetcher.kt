package com.apollographql.apollo.fetcher

import com.apollographql.apollo.api.internal.ApolloLogger
import com.apollographql.apollo.interceptor.ApolloInterceptor

/**
 * A ResponseFetcher is an [ApolloInterceptor] inserted at the beginning of a request chain.
 * It can control how a request is fetched by configuring [com.apollographql.apollo.interceptor.FetchOptions].
 *
 * See [ApolloResponseFetchers] for a basic set of fetchers.
 */
interface ResponseFetcher {
  /**
   * @param logger A [ApolloLogger] to log relevant fetch information.
   * @return The [ApolloInterceptor] that executes the fetch logic.
   */
  fun provideInterceptor(logger: ApolloLogger?): ApolloInterceptor
}