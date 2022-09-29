package com.apollographql.apollo3.cache.normalized.api.internal

import com.apollographql.apollo3.annotations.ApolloInternal
import com.apollographql.apollo3.api.CompiledField
import com.apollographql.apollo3.api.CompiledFragment
import com.apollographql.apollo3.api.CompiledSelection
import com.apollographql.apollo3.api.Executable
import com.apollographql.apollo3.cache.normalized.api.ApolloResolver
import com.apollographql.apollo3.cache.normalized.api.CacheHeaders
import com.apollographql.apollo3.cache.normalized.api.CacheKey
import com.apollographql.apollo3.cache.normalized.api.CacheResolver
import com.apollographql.apollo3.cache.normalized.api.ReadOnlyNormalizedCache
import com.apollographql.apollo3.cache.normalized.api.Record
import com.apollographql.apollo3.cache.normalized.api.ResolverContext
import com.apollographql.apollo3.exception.CacheMissException
import kotlin.jvm.JvmSuppressWildcards

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
    private val cacheResolver: Any,
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
      val parentType: String,
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
  private fun collect(selections: List<CompiledSelection>, parentType: String, typename: String?, state: CollectState) {
    selections.forEach { compiledSelection ->
      when (compiledSelection) {
        is CompiledField -> {
          state.fields.add(compiledSelection)
        }
        is CompiledFragment -> {
          if (typename in compiledSelection.possibleTypes || compiledSelection.typeCondition == parentType) {
            collect(compiledSelection.selections, parentType, typename, state)
          }
        }
      }
    }
  }

  private fun collectAndMergeSameDirectives(
      selections: List<CompiledSelection>,
      parentType: String,
      typename: String?,
  ): List<CompiledField> {
    val state = CollectState()
    collect(selections, parentType, typename, state)
    return state.fields.groupBy { (it.responseName) to it.condition }.values.map {
      it.first().newBuilder().selections(it.flatMap { it.selections }).build()
    }
  }

  fun toMap(): Map<String, Any?> {
    pendingReferences.add(
        PendingReference(
            key = rootKey,
            selections = rootSelections,
            parentType = rootTypename,
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

        val collectedFields = collectAndMergeSameDirectives(pendingReference.selections, pendingReference.parentType, record["__typename"] as? String)

        val map = collectedFields.mapNotNull {
          if (it.shouldSkip(variables.valueMap)) {
            return@mapNotNull null
          }

          val value = when (cacheResolver) {
            is CacheResolver -> cacheResolver.resolveField(it, variables, record, record.key)
            is ApolloResolver -> {
              cacheResolver.resolveField(ResolverContext(it, variables, record, record.key, cacheHeaders))
            }
            else -> throw IllegalStateException()
          }
          value.registerCacheKeys(pendingReference.path + it.responseName, it.selections, it.type.rawType().name)

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
  private fun Any?.registerCacheKeys(path: List<Any>, selections: List<CompiledSelection>, parentType: String) {
    when (this) {
      is CacheKey -> {
        pendingReferences.add(
            PendingReference(
                key = key,
                selections = selections,
                parentType = parentType,
                path = path
            )
        )
      }
      is List<*> -> {
        forEachIndexed { index, value ->
          value.registerCacheKeys(path + index, selections, parentType)
        }
      }
      is Map<*, *> -> {
        @Suppress("UNCHECKED_CAST")
        this as Map<String, @JvmSuppressWildcards Any?>
        val collectedFields = collectAndMergeSameDirectives(selections, parentType, get("__typename") as? String)
        collectedFields.mapNotNull {
          if (it.shouldSkip(variables.valueMap)) {
            return@mapNotNull null
          }

          val value = when (cacheResolver) {
            is CacheResolver -> cacheResolver.resolveField(it, variables, this, "")
            is ApolloResolver -> {
              cacheResolver.resolveField(ResolverContext(it, variables, this, "", cacheHeaders))
            }
            else -> throw IllegalStateException()
          }
          value.registerCacheKeys(path + it.responseName, it.selections, it.type.rawType().name)

          it.responseName to value
        }.toMap()
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

