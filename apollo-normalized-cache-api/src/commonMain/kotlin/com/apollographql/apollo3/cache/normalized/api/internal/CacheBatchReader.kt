package com.apollographql.apollo3.cache.normalized.api.internal

import com.apollographql.apollo3.api.CompiledField
import com.apollographql.apollo3.api.CompiledFragment
import com.apollographql.apollo3.api.CompiledSelection
import com.apollographql.apollo3.api.Executable
import com.apollographql.apollo3.cache.normalized.api.CacheHeaders
import com.apollographql.apollo3.cache.normalized.api.CacheKey
import com.apollographql.apollo3.cache.normalized.api.CacheResolver
import com.apollographql.apollo3.cache.normalized.api.ReadOnlyNormalizedCache
import com.apollographql.apollo3.cache.normalized.api.Record
import com.apollographql.apollo3.exception.CacheMissException

/**
 * A resolver that solves the "N+1" problem by batching all SQL queries at a given depth
 * It respects skip/include directives
 *
 * Returns the data in [toMap]
 */
internal class CacheBatchReader(
    private val cache: ReadOnlyNormalizedCache,
    private val rootKey: String,
    private val variables: Executable.Variables,
    private val cacheResolver: CacheResolver,
    private val cacheHeaders: CacheHeaders,
    private val rootSelections: List<CompiledSelection>,
    private val rootTypename: String,
) {
  /**
   * @param key: the key of the record we need to fetch
   * @param path: the path where this pending reference needs to be inserted
   */
  class PendingReference(
      val key: String,
      val path: List<Any>,
      val selections: List<CompiledSelection>,
      val typeInScope: String,
  )

  /**
   * The objects read from the cache with only the fields that are selected and maybe some values changed
   * The key is the path to the object
   */
  private val data = mutableMapOf<List<Any>, Map<String, Any?>>()

  private val pendingReferences = mutableListOf<PendingReference>()

  private class CollectState {
    val fields = mutableListOf<CompiledField>()
  }

  /**
   *
   */
  private fun collect(selections: List<CompiledSelection>, typeInScope: String, typename: String?, state: CollectState) {
    selections.forEach { compiledSelection ->
      when (compiledSelection) {
        is CompiledField -> {
          state.fields.add(compiledSelection)
        }
        is CompiledFragment -> {
          if (typename in compiledSelection.possibleTypes || compiledSelection.typeCondition == typeInScope) {
            collect(compiledSelection.selections, typeInScope, typename, state)
          }
        }
      }
    }
  }

  private fun collectAndMergeSameDirectives(
      selections: List<CompiledSelection>,
      typeInScope: String,
      typename: String?,
  ): List<CompiledField> {
    val state = CollectState()
    collect(selections, typeInScope, typename, state)
    return state.fields.groupBy { (it.responseName) to it.condition }.values.map {
      it.first().newBuilder().selections(it.flatMap { it.selections }).build()
    }
  }

  fun toMap(): Map<String, Any?> {
    pendingReferences.add(
        PendingReference(
            key = rootKey,
            selections = rootSelections,
            typeInScope = rootTypename,
            path = emptyList()
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
            // This happens the very first time we read the cache
            record = Record(pendingReference.key, emptyMap())
          } else {
            throw CacheMissException(pendingReference.key)
          }
        }

        val collectedFields = collectAndMergeSameDirectives(pendingReference.selections, pendingReference.typeInScope, record["__typename"] as? String)

        val map = collectedFields.mapNotNull {
          if (it.shouldSkip(variables.valueMap)) {
            return@mapNotNull null
          }

          val value = cacheResolver.resolveField(it, variables, record, record.key)

          value.registerCacheKeys(pendingReference.path + it.responseName, it.selections, it.type.leafType().name)

          it.responseName to value
        }.toMap()

        data[pendingReference.path] = map
      }
    }

    @Suppress("UNCHECKED_CAST")
    return data[emptyList()].replaceCacheKeys(emptyList()) as Map<String, Any?>
  }

  /**
   * The path leading to this value
   */
  private fun Any?.registerCacheKeys(path: List<Any>, selections: List<CompiledSelection>, typeInScope: String) {
    when (this) {
      is CacheKey -> {
        pendingReferences.add(
            PendingReference(
                key = key,
                selections = selections,
                typeInScope = typeInScope,
                path = path
            )
        )
      }
      is List<*> -> {
        forEachIndexed { index, value ->
          value.registerCacheKeys(path + index, selections, typeInScope)
        }
      }
    }
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

