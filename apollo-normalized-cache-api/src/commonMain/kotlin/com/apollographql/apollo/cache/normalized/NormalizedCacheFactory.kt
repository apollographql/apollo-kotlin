package com.apollographql.apollo.cache.normalized

/**
 * A Factory used to construct an instance of a [NormalizedCache] configured with the custom scalar adapters set in
 * ApolloClient.Builder#addCustomScalarTypeAdapter(ScalarType, CustomScalarTypeAdapter).
 */
abstract class NormalizedCacheFactory<T : NormalizedCache> {

  private var nextFactory: NormalizedCacheFactory<out NormalizedCache>? = null

  /**
   * @param recordFieldAdapter A [RecordFieldJsonAdapter] configured with the custom scalar adapters set in
   * ApolloClient.Builder#addCustomScalarTypeAdapter(ScalarType, CustomScalarTypeAdapter).
   * @return An implementation of [NormalizedCache].
   */
  abstract fun create(recordFieldAdapter: RecordFieldJsonAdapter): T

  fun createChain(recordFieldAdapter: RecordFieldJsonAdapter): NormalizedCache {
    val nextFactory = nextFactory
    return if (nextFactory != null) {
      create(recordFieldAdapter).chain(nextFactory.createChain(recordFieldAdapter))
    } else {
      create(recordFieldAdapter)
    }
  }

  fun chain(factory: NormalizedCacheFactory<*>) = apply {
    var leafFactory: NormalizedCacheFactory<*> = this
    while (leafFactory.nextFactory != null) {
      leafFactory = leafFactory.nextFactory!!
    }
    leafFactory.nextFactory = factory
  }
}
