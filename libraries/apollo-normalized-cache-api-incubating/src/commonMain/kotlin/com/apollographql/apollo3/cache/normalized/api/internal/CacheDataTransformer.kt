package com.apollographql.apollo3.cache.normalized.api.internal

import com.apollographql.apollo3.annotations.ApolloInternal
import com.apollographql.apollo3.api.CompositeAdapter
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.Executable
import com.apollographql.apollo3.api.fromJson
import com.apollographql.apollo3.api.json.MapJsonReader
import com.apollographql.apollo3.cache.normalized.api.CacheKey

@ApolloInternal
data class CacheDataTransformer<D: Executable.Data>(
    private val adapter: CompositeAdapter<D>,
    private val data: MutableMap<List<Any>, Map<String, Any?>>,
) {
  fun toData(
      customScalarAdapters: CustomScalarAdapters,
  ): D {
    val reader = MapJsonReader(
        root = toMap(),
    )
    return adapter.fromJson(reader, customScalarAdapters)
  }

  @Suppress("UNCHECKED_CAST")
  private fun toMap(): Map<String, Any?> {
    return data[emptyList()].replaceCacheKeys(emptyList()) as Map<String, Any?>
  }

  private fun Any?.replaceCacheKeys(path: List<Any>): Any? {
    return when (this) {
      is CacheKey -> {
        data[path].replaceCacheKeys(path)
      }

      is List<*> -> {
        mapIndexed { index, src ->
          src.replaceCacheKeys(path + index)
        }
      }

      is Map<*, *> -> {
        // This will traverse Map custom scalars but this is ok as it shouldn't contain any CacheKey
        mapValues {
          it.value.replaceCacheKeys(path + (it.key as String))
        }
      }

      else -> {
        // Scalar value
        this
      }
    }
  }
}