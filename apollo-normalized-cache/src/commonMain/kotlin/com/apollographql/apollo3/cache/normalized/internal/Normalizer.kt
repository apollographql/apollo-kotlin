package com.apollographql.apollo3.cache.normalized.internal

import com.apollographql.apollo3.api.CompiledCompoundType
import com.apollographql.apollo3.api.CompiledField
import com.apollographql.apollo3.api.CompiledFragment
import com.apollographql.apollo3.api.CompiledListType
import com.apollographql.apollo3.api.CompiledNotNullType
import com.apollographql.apollo3.api.CompiledSelection
import com.apollographql.apollo3.api.Executable
import com.apollographql.apollo3.api.MergedField
import com.apollographql.apollo3.cache.normalized.CacheReference
import com.apollographql.apollo3.cache.normalized.Record

/**
 * A [Normalizer] takes a [Map]<String, Any?> and turns them into a flat list of [Record]
 * The id of each [Record] is given by [cacheKeyForObject] or defaults to using the path
 */
class Normalizer(val variables: Executable.Variables, val cacheKeyForObject: (CompiledField, Map<String, Any?>) -> String?) {
  private val records = mutableMapOf<String, Record>()
  private val cacheKeyBuilder = RealCacheKeyBuilder()

  fun normalize(map: Map<String, Any?>, path: String?, rootKey: String, selections: List<CompiledSelection>): Map<String, Record> {

    records[rootKey] = Record(rootKey, map.toFields(path, selections = selections))

    return records
  }

  private fun Map<String, Any?>.normalize(path: String, fields: List<CompiledField>): CacheReference {
    val key = cacheKeyForObject(fields.first(), this) ?: path

    val selections = fields.flatMap { it.selections }
    val newRecord = Record(key, toFields(key, selections = selections))

    val existingRecord = records[key]

    val mergedRecord = if (existingRecord != null) {
      /**
       * A query might contain the same object twice, we don't want to lose some fields when that happens
       */
      existingRecord.mergeWith(newRecord).first
    } else {
      newRecord
    }
    records[key] = mergedRecord

    return CacheReference(key)
  }

  private class CollectState {
    val fields = mutableListOf<CompiledField>()
  }

  private fun List<CompiledSelection>.collect(typename: String?, state: CollectState) {
    forEach {
      when (it) {
        is CompiledField -> {
          state.fields.add(it)
          it.selections.collect(typename, state)
        }
        is CompiledFragment -> {
          if (typename in it.possibleTypes) {
            it.selections.collect(typename, state)
          }
        }
      }
    }
  }

  private fun List<CompiledSelection>.collect(typename: String?): List<CompiledField> {
    val state = CollectState()
    collect(typename, state)
    return state.fields
  }

  /**
   * Takes the map entries and replaces compound types values by CacheReferences
   */
  private fun Map<String, Any?>.toFields(path: String?, selections: List<CompiledSelection>): Map<String, Any?> {
    val fields = selections.collect(get("__typename") as? String)

    return entries.mapNotNull { entry ->
      val mergedFields = fields.filter { it.responseName == entry.key }

      val first = mergedFields.firstOrNull()

      check(first != null && (first.type !is CompiledNotNullType || entry.value != null))

      if (mergedFields.all { it.shouldSkip(variableValues = variables.valueMap) }) {
        // GraphQL doesn't distinguish between null and absent so this is null but it might be absent
        // If it is absent, we don't want to serialize it to the cache
        return@mapNotNull null
      }

      val fieldKey = cacheKeyBuilder.build(first, variables)

      val unwrappedType = (first.type as? CompiledNotNullType)?.ofType ?: first.type

      val value = entry.value
      @Suppress("UNCHECKED_CAST")
      fieldKey to when {
        value == null -> null
        unwrappedType is CompiledListType -> (value as List<Any?>).normalize(path.append(fieldKey), mergedFields, unwrappedType)
        unwrappedType is CompiledCompoundType -> (value as Map<String, Any?>).normalize(path.append(fieldKey), mergedFields)
        else -> value // scalar or enum
      }
    }.toMap()
  }

  // The receiver can be null for the root query to save some space in the cache by not storing QUERY_ROOT all over the place
  private fun String?.append(next: String): String = if (this == null) next else "$this.$next"

  /**
   * @param fieldType this is different from field.type as it will unwrap the NonNull and List types as it goes
   */
  @Suppress("UNCHECKED_CAST")
  private fun List<Any?>.normalize(path: String, fields: List<CompiledField>, fieldType: CompiledListType): List<Any?> {
    val unwrappedType = (fieldType.ofType as? CompiledNotNullType)?.ofType ?: fieldType.ofType

    return mapIndexed { index, value ->
      when {
        value == null -> null
        unwrappedType is CompiledListType -> (value as List<Any?>).normalize(path.append(index.toString()), fields, unwrappedType)
        unwrappedType is CompiledCompoundType -> (value as Map<String, Any?>).normalize(path.append(index.toString()), fields)
        else -> value
      }
    }
  }
}