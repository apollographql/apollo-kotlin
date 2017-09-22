package com.apollographql.apollo.fetcher;

import com.apollographql.apollo.interceptor.ApolloInterceptor;
import com.apollographql.apollo.internal.ApolloLogger;

/**
 * A ResponseFetcher is an {@link ApolloInterceptor} inserted at the beginning of a request chain.
 * It can control how a request is fetched by configuring {@link com.apollographql.apollo.interceptor.FetchOptions}.
 *
 * See {@link ApolloResponseFetchers} for a basic set of fetchers.
 */
public interface ResponseFetcher {

  /**
   * @param logger A {@link ApolloLogger} to log relevant fetch information.
   * @return The {@link ApolloInterceptor} that executes the fetch logic.
   */
  ApolloInterceptor provideInterceptor(ApolloLogger logger);

}
