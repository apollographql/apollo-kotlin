package com.apollographql.apollo.cache.normalized

import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.ResponseField

/**
 * Resolves a cache key for a JSON object.
 */
abstract class CacheKeyResolver {
  abstract fun fromFieldRecordSet(
      field: ResponseField,
      recordSet: Map<String, Any?>
  ): CacheKey

  abstract fun fromFieldArguments(
      field: ResponseField,
      variables: Operation.Variables
  ): CacheKey

  companion object {
    private val ROOT_CACHE_KEY = CacheKey("QUERY_ROOT")

    @JvmField
    val DEFAULT: CacheKeyResolver = object : CacheKeyResolver() {
      override fun fromFieldRecordSet(field: ResponseField, recordSet: Map<String, Any?>) = CacheKey.NO_KEY

      override fun fromFieldArguments(field: ResponseField, variables: Operation.Variables) = CacheKey.NO_KEY
    }

    @JvmStatic
    @Suppress("UNUSED_PARAMETER")
    fun rootKeyForOperation(operation: Operation<*, *, *>): CacheKey {
      return ROOT_CACHE_KEY
    }
  }
}
