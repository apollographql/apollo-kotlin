package com.apollographql.apollo3.cache.normalized.api

/**
 * A Factory used to construct an instance of a [NormalizedCache] configured with the custom scalar adapters set in
 * ApolloClient.Builder#addCustomScalarAdapter(ScalarType, CustomScalarAdapter).
 */
abstract class NormalizedCacheFactory {

  /**
   * ApolloClient.Builder#addCustomScalarAdapter(ScalarType, CustomScalarAdapter).
   * @return An implementation of [NormalizedCache].
   */
  abstract fun create(): NormalizedCache
}
