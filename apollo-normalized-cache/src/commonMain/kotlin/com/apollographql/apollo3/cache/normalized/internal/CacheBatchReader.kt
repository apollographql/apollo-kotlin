package com.apollographql.apollo3.cache.normalized.internal

import com.apollographql.apollo3.api.CompiledField
import com.apollographql.apollo3.api.CompiledFragment
import com.apollographql.apollo3.api.CompiledSelection
import com.apollographql.apollo3.api.Executable
import com.apollographql.apollo3.api.exception.CacheMissException
import com.apollographql.apollo3.cache.CacheHeaders
import com.apollographql.apollo3.cache.normalized.CacheKey
import com.apollographql.apollo3.cache.normalized.CacheResolver
import com.apollographql.apollo3.cache.normalized.ReadOnlyNormalizedCache
import com.apollographql.apollo3.cache.normalized.Record

/**
 * A resolver that solves the "N+1" problem by batching all SQL queries at a given depth
 * It respects skip/include directives
 *
 * Returns the data in [toMap]
 */
class CacheBatchReader(
    private val cache: ReadOnlyNormalizedCache,
    private val rootKey: String,
    private val variables: Executable.Variables,
    private val cacheResolver: CacheResolver,
    private val cacheHeaders: CacheHeaders,
    private val rootSelections: List<CompiledSelection>
) {
  class PendingReference(
      val key: String,
      val selections: List<CompiledSelection>
  )

  private val data = mutableMapOf<String, Map<String, Any?>>()

  private val pendingReferences = mutableListOf<PendingReference>()

  private class CollectState {
    val fields = mutableListOf<CompiledField>()
  }

  private fun List<CompiledSelection>.collect(typename: String?, state: CollectState) {
    forEach {
      when(it) {
        is CompiledField -> {
          state.fields.add(it)
        }
        is CompiledFragment -> {
          if (typename in it.possibleTypes) {
            it.selections.collect(typename, state)
          }
        }
      }
    }
  }

  private fun List<CompiledSelection>.collectAndMergeSameDirectives(typename: String?): List<CompiledField> {
    val state = CollectState()
    collect(typename, state)
    return state.fields.groupBy { (it.responseName) to it.condition}.values.map {
      val first = it.first()
      CompiledField(
          alias = first.alias,
          name = first.name,
          type = first.type,
          condition = first.condition,
          arguments = first.arguments,
          selections = it.flatMap { it.selections }
      )
    }
  }

  fun toMap(): Map<String, Any?> {
    pendingReferences.add(
        PendingReference(
            rootKey,
            rootSelections
        )
    )

    while (pendingReferences.isNotEmpty()) {
      val records = cache.loadRecords(pendingReferences.map { it.key }, cacheHeaders).associateBy { it.key }

      val copy = pendingReferences.toList()
      pendingReferences.clear()
      copy.forEach { pendingReference ->
        var record = records[pendingReference.key]
        if (record == null) {
          if (pendingReference.key == CacheKey.rootKey().key) {
            record = Record(pendingReference.key, emptyMap())
          } else {
            throw CacheMissException(pendingReference.key)
          }
        }

        val collectedFields = pendingReference.selections.collectAndMergeSameDirectives(record["__typename"] as? String)

        val map = collectedFields.mapNotNull {
          if (it.shouldSkip(variables.valueMap)) {
            return@mapNotNull null
          }

          val value = cacheResolver.resolveField(it, variables, record, record.key)

          value.registerCacheKeys(it.selections)

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
    return data[rootKey].resolveCacheKeys() as Map<String, Any?>
  }

  private fun Any?.registerCacheKeys(selections: List<CompiledSelection>) {
    when (this) {
      is CacheKey -> {
        pendingReferences.add(PendingReference(key, selections))
      }
      is List<*> -> {
        forEach {
          it.registerCacheKeys(selections)
        }
      }
    }
  }

  private fun Any?.resolveCacheKeys(): Any? {
    return when (this) {
      is CacheKey -> {
        data[key].resolveCacheKeys()
      }
      is List<*> -> {
        map {
          it.resolveCacheKeys()
        }
      }
      is Map<*, *> -> {
        // This will traverse Map custom scalars but this is ok as it shouldn't contain any CacheKey
        mapValues { it.value.resolveCacheKeys() }
      }
      else -> this
    }
  }
}

