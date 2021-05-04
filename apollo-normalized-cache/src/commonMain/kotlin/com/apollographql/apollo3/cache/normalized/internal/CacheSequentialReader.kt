package com.apollographql.apollo3.cache.normalized.internal

import com.apollographql.apollo3.api.Executable
import com.apollographql.apollo3.api.FieldSet
import com.apollographql.apollo3.api.MergedField
import com.apollographql.apollo3.cache.CacheHeaders
import com.apollographql.apollo3.cache.normalized.CacheKey
import com.apollographql.apollo3.cache.normalized.CacheKeyResolver
import com.apollographql.apollo3.cache.normalized.CacheReference
import com.apollographql.apollo3.cache.normalized.ReadOnlyNormalizedCache
import com.apollographql.apollo3.cache.normalized.Record
import com.apollographql.apollo3.api.exception.CacheMissException

class CacheSequentialReader(
    private val cache: ReadOnlyNormalizedCache,
    private val rootKey: String,
    private val variables: Executable.Variables,
    private val cacheKeyResolver: CacheKeyResolver,
    private val cacheHeaders: CacheHeaders,
    private val rootFieldSets: List<FieldSet>) {

  private val cacheKeyBuilder = RealCacheKeyBuilder()

  private fun MergedField.Type.isObject(): Boolean = when (this) {
    is MergedField.Type.NotNull -> ofType.isObject()
    is MergedField.Type.Named.Object -> true
    else -> false
  }

  @Suppress("UNCHECKED_CAST")
  fun toMap(): Map<String, Any?> {
    return CacheReference(rootKey).resolve(rootFieldSets) as Map<String, Any?>
  }

  private fun resolve(record: Record, fieldSets: List<FieldSet>): Map<String, Any?> {
    val fieldSet = fieldSets.firstOrNull { it.type == record["__typename"] }
        ?: fieldSets.first { it.type == null }

    return fieldSet.mergedFields.mapNotNull {
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

      it.responseName to value.resolve(it.fieldSets)
    }.toMap()

  }
  private fun Any?.resolve(fieldSets: List<FieldSet>): Any? {
    return when (this) {
      is CacheReference -> {
        val record = cache.loadRecord(key, cacheHeaders)
        if (record == null) {
          throw CacheMissException(rootKey)
        }
        resolve(record, fieldSets)
      }
      is List<*> -> {
        map {
          it.resolve(fieldSets)
        }
      }
      else -> this
    }
  }
}