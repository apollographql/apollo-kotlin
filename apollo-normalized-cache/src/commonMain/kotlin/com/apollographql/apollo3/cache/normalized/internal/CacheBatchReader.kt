package com.apollographql.apollo3.cache.normalized.internal

import com.apollographql.apollo3.api.Executable
import com.apollographql.apollo3.api.FieldSet
import com.apollographql.apollo3.api.MergedField
import com.apollographql.apollo3.cache.CacheHeaders
import com.apollographql.apollo3.cache.normalized.CacheKey
import com.apollographql.apollo3.cache.normalized.CacheKeyResolver
import com.apollographql.apollo3.cache.normalized.CacheReference
import com.apollographql.apollo3.cache.normalized.ReadOnlyNormalizedCache
import com.apollographql.apollo3.api.exception.CacheMissException

/**
 * Reads [rootFieldSets] starting at [rootKey] from [cache]
 *
 * This is a resolver that solves the "N+1" problem by batching all SQL queries at a given depth
 * It respects skip/include directives
 *
 * Returns the data in [toMap]
 */
class CacheBatchReader(
    private val cache: ReadOnlyNormalizedCache,
    private val rootKey: String,
    private val variables: Executable.Variables,
    private val cacheKeyResolver: CacheKeyResolver,
    private val cacheHeaders: CacheHeaders,
    private val rootFieldSets: List<FieldSet>
) {
  private val cacheKeyBuilder = RealCacheKeyBuilder()

  class PendingReference(
      val key: String,
      val fieldSets: List<FieldSet>
  )

  private val data = mutableMapOf<String, Map<String, Any?>>()

  private val pendingReferences = mutableListOf<PendingReference>()

  private fun MergedField.Type.isObject(): Boolean = when (this) {
    is MergedField.Type.NotNull -> ofType.isObject()
    is MergedField.Type.Named.Object -> true
    else -> false
  }

  fun toMap(): Map<String, Any?> {
    pendingReferences.add(
        PendingReference(
            rootKey,
            rootFieldSets
        )
    )

    while (pendingReferences.isNotEmpty()) {
      val records = cache.loadRecords(pendingReferences.map { it.key }, cacheHeaders).associateBy { it.key }

      val copy = pendingReferences.toList()
      pendingReferences.clear()
      copy.forEach { pendingReference ->
        val record = records[pendingReference.key] ?: throw CacheMissException(pendingReference.key)

        val fieldSet = pendingReference.fieldSets.firstOrNull { it.type == record["__typename"] }
            ?: pendingReference.fieldSets.first { it.type == null }

        val map = fieldSet.mergedFields.mapNotNull {
          if (it.shouldSkip(variables.valueMap)) {
            return@mapNotNull null
          }

          val type = it.type
          val value = if (type.isObject()) {
            val cacheKey = cacheKeyResolver.fromFieldArguments(it, variables)
            if (cacheKey != CacheKey.NO_KEY ) {
              // user provided a lookup
              CacheReference(cacheKey.key)
            } else {
              // no key provided
              val fieldName = cacheKeyBuilder.build(it, variables)
              if (!record.containsKey(fieldName)) {
                throw CacheMissException(record.key, fieldName)
              }
              record[fieldName]
            }
          } else {
            val fieldName = cacheKeyBuilder.build(it, variables)
            if (!record.containsKey(fieldName)) {
              throw CacheMissException(record.key, fieldName)
            }
            record[fieldName]
          }

          value.registerCacheReferences(it.fieldSets)

          it.responseName to value
        }.toMap()

        val existingValue = data[record.key]
        val newValue = if (existingValue != null) {
          existingValue + map
        } else {
          map
        }
        data[record.key] = newValue
      }
    }

    @Suppress("UNCHECKED_CAST")
    return data[rootKey].resolveCacheReferences() as Map<String, Any?>
  }

  private fun Any?.registerCacheReferences(fieldSets: List<FieldSet>) {
    when (this) {
      is CacheReference -> {
        pendingReferences.add(PendingReference(key, fieldSets))
      }
      is List<*> -> {
        forEach {
          it.registerCacheReferences(fieldSets)
        }
      }
    }
  }

  private fun Any?.resolveCacheReferences(): Any? {
    return when (this) {
      is CacheReference -> {
        data[key].resolveCacheReferences()
      }
      is List<*> -> {
        map {
          it.resolveCacheReferences()
        }
      }
      is Map<*, *> -> {
        // This will traverse Map custom scalars but this is ok as it shouldn't contain any CacheReference
        mapValues { it.value.resolveCacheReferences() }
      }
      else -> this
    }
  }
}

