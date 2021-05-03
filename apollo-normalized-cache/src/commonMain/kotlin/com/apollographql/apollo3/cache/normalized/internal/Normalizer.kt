package com.apollographql.apollo3.cache.normalized.internal

import com.apollographql.apollo3.api.Executable
import com.apollographql.apollo3.api.MergedField
import com.apollographql.apollo3.cache.normalized.CacheReference
import com.apollographql.apollo3.cache.normalized.Record

class Normalizer(val variables: Executable.Variables, val cacheKeyForObject: (MergedField, Map<String, Any?>) -> String?) {
  private val records = mutableMapOf<String, Record>()
  private val cacheKeyBuilder = RealCacheKeyBuilder()

  fun normalize(map: Map<String, Any?>, path: String?, rootKey: String, fieldSets: List<MergedField.FieldSet>): Map<String, Record> {

    records[rootKey] = Record(rootKey, map.toFields(path, fieldSets = fieldSets))

    return records
  }

  private fun Map<String, Any?>.normalize(path: String, field: MergedField): CacheReference {
    val key = cacheKeyForObject(field, this) ?: path

    val newRecord = Record(key, toFields(key, fieldSets = field.fieldSets))

    val existingRecord = records[key]
    val mergedRecord = if (existingRecord != null) {
      existingRecord.mergeWith(newRecord).first
    } else {
      newRecord
    }
    records[key] = mergedRecord

    return CacheReference(key)
  }

  private fun Map<String, Any?>.toFields(path: String?, fieldSets: List<MergedField.FieldSet>): Map<String, Any?> {
    val fieldSet = fieldSets.firstOrNull { it.typeCondition == get("__typename") }
        ?: fieldSets.firstOrNull { it.typeCondition == null }

    check(fieldSet != null) {
      "No field set found at $path on typeCondition $this"
    }
    return fieldSet.mergedFields.mapNotNull {
      if (it.shouldSkip(variableValues = variables.valueMap)) {
        // The json doesn't know about skip/include so filter here
        return@mapNotNull null
      }

      val value = get(it.responseName)

      check(it.type !is MergedField.Type.NotNull || value != null)

      val fieldKey = cacheKeyBuilder.build(it, variables)

      val unwrappedType = (it.type as? MergedField.Type.NotNull)?.ofType ?: it.type

      @Suppress("UNCHECKED_CAST")
      fieldKey to when {
        value == null -> null
        unwrappedType is MergedField.Type.List -> (value as List<Any?>).normalize(path.append(fieldKey), it, unwrappedType)
        unwrappedType is MergedField.Type.Named.Object -> (value as Map<String, Any?>).normalize(path.append(fieldKey), it)
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
  private fun List<Any?>.normalize(path: String, field: MergedField, fieldType: MergedField.Type.List): List<Any?> {
    val unwrappedType = (fieldType.ofType as? MergedField.Type.NotNull)?.ofType ?: fieldType.ofType

    return mapIndexed { index, value ->
      when {
        value == null -> null
        unwrappedType is MergedField.Type.List -> (value as List<Any?>).normalize(path.append(index.toString()), field, unwrappedType)
        unwrappedType is MergedField.Type.Named.Object -> (value as Map<String, Any?>).normalize(path.append(index.toString()), field)
        else -> value
      }
    }
  }
}