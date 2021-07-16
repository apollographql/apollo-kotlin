package com.apollographql.apollo3.cache.normalized.internal

import com.apollographql.apollo3.api.CompiledField
import com.apollographql.apollo3.api.CompiledFragment
import com.apollographql.apollo3.api.CompiledListType
import com.apollographql.apollo3.api.CompiledNamedType
import com.apollographql.apollo3.api.CompiledNotNullType
import com.apollographql.apollo3.api.CompiledSelection
import com.apollographql.apollo3.api.CompiledType
import com.apollographql.apollo3.api.Executable
import com.apollographql.apollo3.api.isComposite
import com.apollographql.apollo3.cache.normalized.CacheKey
import com.apollographql.apollo3.cache.normalized.ObjectIdGenerator
import com.apollographql.apollo3.cache.normalized.ObjectIdGeneratorContext
import com.apollographql.apollo3.cache.normalized.Record

/**
 * A [Normalizer] takes a [Map]<String, Any?> and turns them into a flat list of [Record]
 * The key of each [Record] is given by [objectIdGenerator] or defaults to using the path
 */
class Normalizer(
    private val variables: Executable.Variables,
    private val rootKey: String,
    private val objectIdGenerator: ObjectIdGenerator
)  {
  private val records = mutableMapOf<String, Record>()

  fun normalize(map: Map<String, Any?>, selections: List<CompiledSelection>): Map<String, Record> {
    buildRecord(map, rootKey, selections)

    return records
  }

  /**
   * @param obj the json node representing the object
   * @param key the key for this record
   * @param selections the selections queried on this object
   * @return the CacheKey
   */
  private fun buildRecord(
      obj: Map<String, Any?>,
      key: String,
      selections: List<CompiledSelection>
  ): CacheKey {

    val allFields = collectFields(selections, obj["__typename"] as? String)

    val record = Record(
        key = key,
        fields = obj.entries.mapNotNull { entry ->
          val mergedFields = allFields.filter { it.responseName == entry.key }
          val merged = mergedFields.first().copy(
              selections = mergedFields.flatMap { it.selections }
          )
          val fieldKey = merged.nameWithArguments(variables)

          if (mergedFields.all { it.shouldSkip(variableValues = variables.valueMap) }) {
            // GraphQL doesn't distinguish between null and absent so this is null but it might be absent
            // If it is absent, we don't want to serialize it to the cache
            return@mapNotNull null
          }

          val base = if (key == rootKey) {
            // If we're at the root level, skip `QUERY_ROOT` altogether to save a few bytes
            null
          } else {
            key
          }

          fieldKey to replaceObjects(
              entry.value,
              merged,
              merged.type,
              base.append(fieldKey),
          )
        }.toMap()
    )

    val existingRecord = records[key]

    val mergedRecord = if (existingRecord != null) {
      /**
       * A query might contain the same object twice, we don't want to lose some fields when that happens
       */
      existingRecord.mergeWith(record).first
    } else {
      record
    }
    records[key] = mergedRecord

    return CacheKey(key)
  }


  /**
   * @param field the field currently being normalized
   * @param type_ the type currently being normalized. It can be different from [field.type] for lists.
   * Since the same field will be used for several objects in list, we can't map 1:1 anymore
   */
  private fun replaceObjects(
      value: Any?,
      field: CompiledField,
      type_: CompiledType,
      path: String,
  ): Any? {
    /**
     * Remove the NotNull decoration if needed
     */
    val type = if (type_ is CompiledNotNullType) {
      check(value != null)
      type_.ofType
    } else {
      if (value == null) {
        return null
      }
      type_
    }

    return when {
      type is CompiledListType -> {
        check(value is List<*>)
        value.mapIndexed { index, item ->
          replaceObjects(item, field, type.ofType, path.append(index.toString()))
        }
      }
      // Check for [isComposite] as we don't want to build a record for json scalars
      type is CompiledNamedType && type.isComposite() -> {
        check(value is Map<*, *>)
        @Suppress("UNCHECKED_CAST")
        val key = objectIdGenerator.cacheKeyForObject(
            value as Map<String, Any?>,
            ObjectIdGeneratorContext(field, variables),
        )?.key ?: path
        buildRecord(value, key, field.selections)
      }
      else -> {
        // scalar
        value
      }
    }
  }

  private class CollectState {
    val fields = mutableListOf<CompiledField>()
  }

  private fun collectFields(selections: List<CompiledSelection>, typename: String?, state: CollectState) {
    selections.forEach {
      when (it) {
        is CompiledField -> {
          state.fields.add(it)
        }
        is CompiledFragment -> {
          if (typename in it.possibleTypes) {
            collectFields(it.selections, typename, state)
          }
        }
      }
    }
  }

  /**
   * @param typename the typename of the object. It might be null if the `__typename` field wasn't queried. If
   * that's the case, we will collect less fields we should and records will miss some values leading to more
   * cache miss
   */
  private fun collectFields(selections: List<CompiledSelection>, typename: String?): List<CompiledField> {
    val state = CollectState()
    collectFields(selections, typename, state)
    return state.fields
  }

  // The receiver can be null for the root query to save some space in the cache by not storing QUERY_ROOT all over the place
  private fun String?.append(next: String): String = if (this == null) next else "$this.$next"
}

