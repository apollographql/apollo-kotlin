package com.apollographql.apollo3.cache.normalized.api

/**
 * A Factory used to construct an instance of a [NormalizedCache] configured with the custom scalar adapters set in
 * ApolloClient.Builder#addCustomScalarAdapter(ScalarType, CustomScalarAdapter).
 */
abstract class NormalizedCacheFactory {

  private var nextFactory: NormalizedCacheFactory? = null

  /**
   * ApolloClient.Builder#addCustomScalarAdapter(ScalarType, CustomScalarAdapter).
   * @return An implementation of [NormalizedCache].
   */
  abstract fun create(): NormalizedCache

  fun createChain(): NormalizedCache {
    val nextFactory = nextFactory
    return if (nextFactory != null) {
      create().chain(nextFactory.createChain())
    } else {
      create()
    }
  }

  fun chain(factory: NormalizedCacheFactory) = apply {
    var leafFactory: NormalizedCacheFactory = this
    while (leafFactory.nextFactory != null) {
      leafFactory = leafFactory.nextFactory!!
    }
    leafFactory.nextFactory = factory
  }
}
