package com.apollographql.apollo3.cache.normalized.api.internal

import com.apollographql.apollo3.api.CompiledSchema
import com.apollographql.apollo3.api.Executable
import com.apollographql.apollo3.ast.GQLField
import com.apollographql.apollo3.ast.GQLFragmentDefinition
import com.apollographql.apollo3.ast.GQLFragmentSpread
import com.apollographql.apollo3.ast.GQLInlineFragment
import com.apollographql.apollo3.ast.GQLOperationDefinition
import com.apollographql.apollo3.ast.GQLSelection
import com.apollographql.apollo3.ast.parseAsGQLDocument
import com.apollographql.apollo3.ast.responseName
import com.apollographql.apollo3.ast.toUtf8
import com.apollographql.apollo3.cache.normalized.api.CacheHeaders
import com.apollographql.apollo3.cache.normalized.api.CacheKey
import com.apollographql.apollo3.cache.normalized.api.CacheResolver
import com.apollographql.apollo3.cache.normalized.api.ReadOnlyNormalizedCache
import com.apollographql.apollo3.cache.normalized.api.Record
import com.apollographql.apollo3.exception.CacheMissException
import okio.Buffer

/**
 * A resolver that solves the "N+1" problem by batching all SQL queries at a given depth
 * It respects skip/include directives
 *
 * Returns the data in [toMap]
 */
internal class CacheBatchReader2(
    private val cache: ReadOnlyNormalizedCache,
    private val rootKey: String,
    private val variables: Executable.Variables,
    private val cacheResolver: CacheResolver,
    private val cacheHeaders: CacheHeaders,
    graphqlDocument: String,
    private val compiledSchema: CompiledSchema,
) {
  private val fragmentDefinitions: Map<String, GQLFragmentDefinition>
  private val rootSelections: List<GQLSelection>

  init {
    val operationDefinitions = mutableListOf<GQLOperationDefinition>()
    val fragmentDefinitions = mutableListOf<GQLFragmentDefinition>()

    Buffer().writeUtf8(graphqlDocument)
        .parseAsGQLDocument()
        .getOrThrow()
        .definitions
        .forEach {
          when (it) {
            is GQLOperationDefinition -> operationDefinitions.add(it)
            is GQLFragmentDefinition -> fragmentDefinitions.add(it)
            else -> throw IllegalStateException("unexpected definition: ${it.toUtf8()}")
          }
        }

    rootSelections = if (operationDefinitions.isNotEmpty()) {
      operationDefinitions.single().selections
    } else {
      fragmentDefinitions.single().selections
    }

    this.fragmentDefinitions = fragmentDefinitions.associateBy { it.name }
  }


  /**
   * @param key: the key of the record we need to fetch
   * @param path: the path where this pending reference needs to be inserted
   */
  class PendingReference(
      val key: String,
      val path: List<Any>,
      val selections: List<GQLSelection>,
  )

  /**
   * The objects read from the cache with only the fields that are selected and maybe some values changed
   * The key is the path to the object
   */
  private val data = mutableMapOf<List<Any>, Map<String, Any?>>()

  private val pendingReferences = mutableListOf<PendingReference>()

  private class CollectState {
    val fields = mutableListOf<GQLField>()
  }

  /**
   *
   */
  private fun collect(selections: List<GQLSelection>, superTypes: Set<String>, state: CollectState) {
    selections.forEach { selection ->
      when (selection) {
        is GQLField -> {
          state.fields.add(selection)
        }
        is GQLInlineFragment -> {
          val typeCondition = selection.typeCondition
          if (typeCondition == null || typeCondition.name in superTypes) {
            collect(selection.selections, superTypes, state)
          }
        }
        is GQLFragmentSpread -> {

        }
      }
    }
  }

  private fun collectAndMergeSameDirectives(
      selections: List<GQLSelection>,
      typename: String,
  ): List<GQLField> {
    val state = CollectState()
    val superTypes = compiledSchema.superTypes(typename)
    if (superTypes == null) {
      return emptyList()
    }
    collect(selections, superTypes, state)
    return state.fields.groupBy { (it.responseName())  }.values.map {
      it.first().copy(
          selections = it.flatMap { it.selections }
      )
    }
  }

  fun toMap(): Map<String, Any?> {
    pendingReferences.add(
        PendingReference(
            key = rootKey,
            selections = rootSelections,
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

        val typename = record["__typename"] as? String ?: throw CacheMissException(pendingReference.key)

        val collectedFields = collectAndMergeSameDirectives(pendingReference.selections, typename)

        val map = collectedFields.mapNotNull {
          if (it.shouldSkip(variables.valueMap)) {
            return@mapNotNull null
          }

          val value = cacheResolver.resolveField(it.toCompiledField(), variables, record, record.key)

          value.registerCacheKeys(pendingReference.path + it.responseName(), it.selections)

          it.responseName() to value
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
  private fun Any?.registerCacheKeys(path: List<Any>, selections: List<GQLSelection>) {
    when (this) {
      is CacheKey -> {
        pendingReferences.add(
            PendingReference(
                key = key,
                selections = selections,
                path = path
            )
        )
      }
      is List<*> -> {
        forEachIndexed { index, value ->
          value.registerCacheKeys(path + index, selections)
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

