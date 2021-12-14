package com.apollographql.apollo3.cache.normalized.api

import com.apollographql.apollo3.api.CompiledField
import com.apollographql.apollo3.api.CompiledListType
import com.apollographql.apollo3.api.CompiledNamedType
import com.apollographql.apollo3.api.CompiledNotNullType
import com.apollographql.apollo3.api.Executable
import com.apollographql.apollo3.api.isComposite
import kotlin.jvm.JvmSuppressWildcards

/**
 * A [CacheResolver] that resolves objects and list of objects and fallbacks to the default resolver for scalar fields.
 * It is intended to simplify the usage of [CacheResolver] when no special handling is needed for scalar fields.
 *
 * Override [cacheKeyForField] to compute a cache key for a field of composite type.
 * Override [listOfCacheKeysForField] to compute a list of cache keys for a field of 'list-of-composite' type.
 *
 * For simplicity, this only handles one level of list. Implement [CacheResolver] if you need arbitrary nested lists of objects.
 */
abstract class CacheKeyResolver : CacheResolver {
  /**
   * Return the computed the cache key for a composite field.
   *
   * If the field is of object type, you can get the object typename with `field.type.leafType().name`
   * If the field is of interface type, the concrete object typename is not predictable and the returned [CacheKey] must be unique
   * in the whole schema as it cannot be namespaced by the typename anymore.
   *
   * If the returned [CacheKey] is null, the resolver will use the default handling and use any previously cached value.
   */
  abstract fun cacheKeyForField(field: CompiledField, variables: Executable.Variables): CacheKey?

  /**
   * For a field that contains a list of objects, [listOfCacheKeysForField ] returns a list of [CacheKey]s where each [CacheKey] identifies an object.
   *
   * If the field is of object type, you can get the object typename with `field.type.leafType().name`
   * If the field is of interface type, the concrete object typename is not predictable and the returned [CacheKey] must be unique
   * in the whole schema as it cannot be namespaced by the typename anymore.
   *
   * If an individual [CacheKey] is null, the resulting object will be null in the response.
   * If the returned list of [CacheKey]s is null, the resolver will use the default handling and use any previously cached value.
   */
  open fun listOfCacheKeysForField(field: CompiledField, variables: Executable.Variables): List<CacheKey?>? = null

  final override fun resolveField(
      field: CompiledField,
      variables: Executable.Variables,
      parent: Map<String, @JvmSuppressWildcards Any?>,
      parentId: String,
  ): Any? {
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

    return DefaultCacheResolver.resolveField(field, variables, parent, parentId)
  }
}
