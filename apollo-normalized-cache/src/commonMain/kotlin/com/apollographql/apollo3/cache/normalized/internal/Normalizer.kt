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
import com.apollographql.apollo3.cache.normalized.Record

/**
 * A [Normalizer] takes a [Map]<String, Any?> and turns them into a flat list of [Record]
 * The key of each [Record] is given by [cacheKeyForObject] or defaults to using the path
 */
class Normalizer(
    val variables: Executable.Variables,
    val rootKey: String,
    val cacheKeyForObject: (CompiledNamedType, Map<String, Any?>) -> String?
) {
  private val records = mutableMapOf<String, Record>()

  fun normalize(map: Map<String, Any?>, selections: List<CompiledSelection>): Map<String, Record> {

    buildRecord(map, null, null, selections)

    return records
  }

  /**
   * @param obj the json node representing the object
   * @param path the path to this object from the root. Might be different from the json path
   * @param selections the selections queried on this object
   */
  private fun buildRecord(obj: Map<String, Any?>, path: String?, type: CompiledNamedType?, selections: List<CompiledSelection> ): CacheKey {
    val key = if (type == null) {
      rootKey
    } else {
      cacheKeyForObject(type, obj) ?: path!!
    }

    val allFields = collectFields(selections, obj["__typename"] as? String)

    val record = Record(
        key = key,
        fields = obj.entries.mapNotNull { entry ->
          val mergedFields = allFields.filter { it.responseName == entry.key }
          val first = mergedFields.first()
          val fieldKey = first.nameWithArguments(variables)

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
              first.type,
              base.append(fieldKey),
              mergedFields.flatMap { it.selections }
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


  private fun replaceObjects(value: Any?, type_: CompiledType, path: String, selections: List<CompiledSelection>): Any? {
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
          replaceObjects(item, type.ofType, path.append(index.toString()), selections)
        }
      }
      // Check for [isCompound] as we don't want to build a record for json scalars
      type is CompiledNamedType && type.isComposite() -> {
        check(value is Map<*, *>)
        buildRecord(value as Map<String, Any?>, path, type, selections)
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