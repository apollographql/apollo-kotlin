package com.apollographql.apollo.cache.normalized.internal

import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.ResponseField.Type.LIST
import com.apollographql.apollo.api.ResponseField.Type.OBJECT
import com.apollographql.apollo.api.internal.FieldValueResolver
import com.apollographql.apollo.cache.CacheHeaders
import com.apollographql.apollo.cache.normalized.CacheKey.Companion.NO_KEY
import com.apollographql.apollo.cache.normalized.CacheKeyResolver
import com.apollographql.apollo.cache.normalized.CacheReference
import com.apollographql.apollo.cache.normalized.Record

class CacheFieldValueResolver(
    private val readableCache: ReadableStore,
    private val variables: Operation.Variables,
    private val cacheKeyResolver: CacheKeyResolver,
    private val cacheHeaders: CacheHeaders,
    private val cacheKeyBuilder: CacheKeyBuilder
) : FieldValueResolver<Record> {

  @Suppress("UNCHECKED_CAST")
  override fun <T> valueFor(recordSet: Record, field: ResponseField): T? {
    return when (field.type) {
      OBJECT -> valueForObject(recordSet, field) as T?
      LIST -> valueForList(fieldValue(recordSet, field)) as T?
      else -> fieldValue(recordSet, field)
    }
  }

  private fun valueForObject(record: Record, field: ResponseField): Record? {
    val cacheReference: CacheReference? =
        when (val fieldCacheKey = cacheKeyResolver.fromFieldArguments(field, variables)) {
          NO_KEY -> fieldValue(record, field)
          else -> CacheReference(fieldCacheKey.key)
        }
    return cacheReference?.let {
      readableCache.read(cacheReference.key, cacheHeaders)
          ?: // we are unable to find record in the cache by reference,
          // means it was removed intentionally by using imperative store API or
          // evicted from LRU cache, we must prevent of further resolving cache response as it's broken
          error("Cache MISS: failed to find record in cache by reference")
    }
  }

  private fun valueForList(values: List<*>?): List<*>? {
    return values?.map { value ->
      when (value) {
        is CacheReference -> {
          readableCache.read(value.key, cacheHeaders)
              ?: // we are unable to find record in the cache by reference,
              // means it was removed intentionally by using imperative store API or
              // evicted from LRU cache, we must prevent of further resolving cache response as it's broken
              error("Cache MISS: failed to find record in cache by reference")
        }
        is List<*> -> valueForList(value)
        else -> value
      }
    }
  }

  @Suppress("UNCHECKED_CAST")
  private fun <T> fieldValue(record: Record, field: ResponseField): T? {
    val fieldKey = cacheKeyBuilder.build(field, variables)
    if (!record.hasField(fieldKey)) {
      throw CacheMissException(record, field.fieldName)
    }
    return record.field(fieldKey) as T?
  }

}
