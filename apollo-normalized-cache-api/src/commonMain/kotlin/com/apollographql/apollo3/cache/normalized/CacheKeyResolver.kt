package com.apollographql.apollo3.cache.normalized

import com.apollographql.apollo3.api.CompiledField
import com.apollographql.apollo3.api.CompiledListType
import com.apollographql.apollo3.api.CompiledNamedType
import com.apollographql.apollo3.api.CompiledNotNullType
import com.apollographql.apollo3.api.Executable
import com.apollographql.apollo3.api.isComposite

/**
 * A [CacheResolver] that only works with [CacheKey]s. This is intended to simplify the usage of [CacheResolver].
 *
 * Override [cacheKeyForField] to compute a cache key for an object field.
 * Override [listOfCacheKeysForField] to compute a list of cache keys for a field that contains a list of objects.
 *
 * For simplicity, this only handles one level of list. Subclass [CacheResolver] if you need arbitrary nested lists of objects.
 */
abstract class CacheKeyResolver : CacheResolver() {

  /**
   * Return the computed the cache key for an object field.
   *
   * Return `null` if this field does not need a special cache key.
   */
  abstract fun cacheKeyForField(field: CompiledField, variables: Executable.Variables): CacheKey?

  /**
   * For a field that has a list of objects, this function should return the cache key computed for each object.
   *
   * Return `null` if this field does not need a special cache key.
   */
  abstract fun listOfCacheKeysForField(field: CompiledField, variables: Executable.Variables): List<CacheKey?>?

  final override fun resolveField(field: CompiledField, variables: Executable.Variables, parent: Map<String, Any?>, parentKey: String): Any? {
    var type = field.type
    if (type is CompiledNotNullType) {
      type = type.ofType
    }
    if (type is CompiledNamedType && type.isComposite()) {
      val result = cacheKeyForField(field, variables)
      if (result != null) {
        return result
      }
    }

    if (type is CompiledListType) {
      type = type.ofType
      if (type is CompiledNotNullType) {
        type = type.ofType
      }
      if (type is CompiledNamedType && type.isComposite()) {
        val result = listOfCacheKeysForField(field, variables)
        if (result != null) {
          return result
        }
      }
    }

    return super.resolveField(field, variables, parent, parentKey)
  }
}
