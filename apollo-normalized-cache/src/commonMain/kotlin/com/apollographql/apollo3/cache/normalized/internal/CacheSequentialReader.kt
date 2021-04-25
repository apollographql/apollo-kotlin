package com.apollographql.apollo3.cache.normalized.internal

import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.ResponseField
import com.apollographql.apollo3.cache.CacheHeaders
import com.apollographql.apollo3.cache.normalized.CacheKey
import com.apollographql.apollo3.cache.normalized.CacheKeyResolver
import com.apollographql.apollo3.cache.normalized.CacheReference
import com.apollographql.apollo3.cache.normalized.ReadOnlyNormalizedCache
import com.apollographql.apollo3.cache.normalized.Record
import com.apollographql.apollo3.exception.CacheMissException

class CacheSequentialReader(
    private val cache: ReadOnlyNormalizedCache,
    private val rootKey: String,
    private val variables: Operation.Variables,
    private val cacheKeyResolver: CacheKeyResolver,
    private val cacheHeaders: CacheHeaders,
    private val rootFieldSets: List<ResponseField.FieldSet>) {

  private val cacheKeyBuilder = RealCacheKeyBuilder()

  private fun ResponseField.Type.isObject(): Boolean = when (this) {
    is ResponseField.Type.NotNull -> ofType.isObject()
    is ResponseField.Type.Named.Object -> true
    else -> false
  }

  @Suppress("UNCHECKED_CAST")
  fun toMap(): Map<String, Any?> {
    return CacheReference(rootKey).resolve(rootFieldSets) as Map<String, Any?>
  }

  private fun resolve(record: Record, fieldSets: List<ResponseField.FieldSet>): Map<String, Any?> {
    val fieldSet = fieldSets.firstOrNull { it.typeCondition == record["__typename"] }
        ?: fieldSets.first { it.typeCondition == null }

    return fieldSet.responseFields.mapNotNull {
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
  private fun Any?.resolve(fieldSets: List<ResponseField.FieldSet>): Any? {
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