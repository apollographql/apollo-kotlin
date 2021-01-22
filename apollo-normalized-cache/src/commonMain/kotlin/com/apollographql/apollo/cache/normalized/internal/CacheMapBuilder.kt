package com.apollographql.apollo.cache.normalized.internal

import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.cache.CacheHeaders
import com.apollographql.apollo.cache.normalized.CacheKey
import com.apollographql.apollo.cache.normalized.CacheKeyResolver
import com.apollographql.apollo.cache.normalized.CacheReference

class CacheMapBuilder(
    private val readableStore: ReadableStore,
    private val rootKey: String,
    private val variables: Operation.Variables,
    private val cacheKeyResolver: CacheKeyResolver,
    private val cacheHeaders: CacheHeaders,
    private val rootResponseFields: Array<ResponseField>
) {
  private val cacheKeyBuilder = RealCacheKeyBuilder()

  class PendingReference(
      val key: String,
      val possibleResponseFields: Map<String, Array<ResponseField>>
  )

  private val fieldSets = mutableMapOf<String, Map<String, Any?>>()

  private val pendingReferences = mutableListOf<PendingReference>()

  private fun ResponseField.Type.isObject(): Boolean = when (this) {
    is ResponseField.Type.NotNull -> ofType.isObject()
    is ResponseField.Type.Named -> kind == ResponseField.Kind.OBJECT
    else -> false
  }

  fun toMap(): Map<String, Any?> {
    pendingReferences.add(
        PendingReference(
            rootKey,
            mapOf("" to rootResponseFields)
        )
    )

    while (pendingReferences.isNotEmpty()) {
      val records = readableStore.read(pendingReferences.map { it.key }, cacheHeaders).associateBy { it.key }

      val copy = pendingReferences.toList()
      pendingReferences.clear()
      copy.forEach { pendingReference ->
        val record = records[pendingReference.key] ?: throw error("Cache miss on ${pendingReference.key}")

        val responseFields = pendingReference.possibleResponseFields[record["__typename"]]
            ?: pendingReference.possibleResponseFields[""]

        val fieldSet = responseFields!!.map {
          val type = it.type
          val value: Any? = if (type.isObject()) {
            val cacheKey = cacheKeyResolver.fromFieldArguments(it, variables)
            if (cacheKey != CacheKey.NO_KEY ) {
              // user provided a lookup
              record[cacheKey.key]
              // should we fallback to fieldName here?
            } else {
              val fieldName = cacheKeyBuilder.build(it, variables)
              // no key provided
              record[fieldName]
            }
          } else {
            // not an object, use the regular method
            val fieldName = cacheKeyBuilder.build(it, variables)
            record[fieldName]
          }

          value.registerCacheReferences(it.possibleFieldSets)

          it.responseName to value
        }.toMap()

        fieldSets[record.key] = fieldSet
      }
    }

    @Suppress("UNCHECKED_CAST")
    return fieldSets[rootKey].resolveCacheReferences() as Map<String, Any?>
  }

  private fun Any?.registerCacheReferences(possibleFieldSets: Map<String, Array<ResponseField>>) {
    when (this) {
      is CacheReference -> {
        pendingReferences.add(PendingReference(key, possibleFieldSets))
      }
      is List<*> -> {
        forEach {
          it.registerCacheReferences(possibleFieldSets)
        }
      }
    }
  }

  private fun Any?.resolveCacheReferences(): Any? {
    return when (this) {
      is CacheReference -> {
        fieldSets[key].resolveCacheReferences()
      }
      is List<*> -> {
        map {
          it.resolveCacheReferences()
        }
      }
      is Map<*, *> -> {
        mapValues { it.value.resolveCacheReferences() }
      }
      else -> this
    }
  }
}

