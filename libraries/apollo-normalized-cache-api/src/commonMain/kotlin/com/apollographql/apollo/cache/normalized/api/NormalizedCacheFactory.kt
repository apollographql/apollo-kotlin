@file:Suppress("DEPRECATION")

package com.apollographql.apollo.cache.normalized.api

import com.apollographql.apollo.annotations.ApolloDeprecatedSince

/**
 * A Factory used to construct an instance of a [NormalizedCache] configured with the custom scalar adapters set in
 * ApolloClient.Builder#addCustomScalarAdapter(CustomScalarType, Adapter).
 */
@Deprecated("Use the new Normalized Cache at https://github.com/apollographql/apollo-kotlin-normalized-cache")
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v5_0_0)
abstract class NormalizedCacheFactory {

  private var nextFactory: NormalizedCacheFactory? = null

  /**
   * ApolloClient.Builder#addCustomScalarAdapter(CustomScalarType, Adapter).
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
