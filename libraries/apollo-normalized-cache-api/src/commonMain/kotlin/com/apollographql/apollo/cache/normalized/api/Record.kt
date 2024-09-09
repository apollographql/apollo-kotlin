package com.apollographql.apollo.cache.normalized.api

import com.apollographql.apollo.annotations.ApolloExperimental
import com.apollographql.apollo.annotations.ApolloInternal
import com.apollographql.apollo.cache.normalized.api.internal.RecordWeigher.calculateBytes
import com.benasher44.uuid.Uuid

/**
 * A normalized entry that corresponds to a response object. Object fields are stored if they are a GraphQL Scalars. If
 * a field is a GraphQL Object a [CacheKey] will be stored instead.
 */
class Record(
    val key: String,
    /**
     * a list of fields. Values can be
     * - Int
     * - Long
     * - Double
     * - Boolean
     * - String
     * - List
     * - CacheKey (for composite types)
     * - Map (for custom scalars)
     * - null
     */
    val fields: Map<String, RecordValue>,
    val mutationId: Uuid? = null,
) : Map<String, Any?> by fields {

  @ApolloExperimental
  var date: Map<String, Long?>? = null
    private set

  @ApolloInternal
  constructor(
      key: String,
      fields: Map<String, Any?>,
      mutationId: Uuid?,
      date: Map<String, Long?>,
  ) : this(key, fields, mutationId) {
    this.date = date
  }

  val sizeInBytes: Int
    get() {
      val datesSize = date?.size?.times(8) ?: 0
      return calculateBytes(this) + datesSize
    }

  /**
   * Returns a merge result record and a set of field keys which have changed, or were added.
   * A field key incorporates any GraphQL arguments in addition to the field name.
   */
  @ApolloExperimental
  fun mergeWith(newRecord: Record, newDate: Long?): Pair<Record, Set<String>> {
    val changedKeys = mutableSetOf<String>()
    val mergedFields = fields.toMutableMap()
    val date = this.date?.toMutableMap() ?: mutableMapOf()

    for ((fieldKey, newFieldValue) in newRecord.fields) {
      val hasOldFieldValue = fields.containsKey(fieldKey)
      val oldFieldValue = fields[fieldKey]
      if (!hasOldFieldValue || oldFieldValue != newFieldValue) {
        mergedFields[fieldKey] = newFieldValue
        changedKeys.add("$key.$fieldKey")
      }
      // Even if the value did not change update date
      if (newDate != null) {
        date[fieldKey] = newDate
      }
    }

    return Record(
        key = key,
        fields = mergedFields,
        mutationId = newRecord.mutationId,
        date = date
    ) to changedKeys
  }

  fun mergeWith(newRecord: Record): Pair<Record, Set<String>> {
    return mergeWith(newRecord, null)
  }


  /**
   * Returns a set of all field keys.
   * A field key incorporates any GraphQL arguments in addition to the field name.
   */
  fun fieldKeys(): Set<String> {
    return fields.keys.map { "$key.$it" }.toSet()
  }

  /**
   * Returns the list of referenced cache fields
   */
  fun referencedFields(): List<CacheKey> {
    val result = mutableListOf<CacheKey>()
    val stack = fields.values.toMutableList()
    while (stack.isNotEmpty()) {
      when (val value = stack.removeAt(stack.size - 1)) {
        is CacheKey -> result.add(value)
        is Map<*, *> -> stack.addAll(value.values)
        is List<*> -> stack.addAll(value)
      }
    }
    return result
  }

  companion object {
    internal fun changedKeys(record1: Record, record2: Record): Set<String> {
      check(record1.key == record2.key) {
        "Cannot compute changed keys on record with different keys: '${record1.key}' - '${record2.key}'"
      }
      val keys1 = record1.fields.keys
      val keys2 = record2.fields.keys
      val intersection = keys1.intersect(keys2)

      val changed = (keys1 - intersection) + (keys2 - intersection) + intersection.filter {
        record1.fields[it] != record2.fields[it]
      }

      return changed.map { "${record1.key}.$it" }.toSet()
    }
  }
}

/**
 * A typealias for a type-unsafe Kotlin representation of a Record value. This typealias is
 * mainly for internal documentation purposes and low-level manipulations and should
 * generally be avoided in application code.
 *
 * [RecordValue] can be any of:
 * - [com.apollographql.apollo.api.json.ApolloJsonElement]
 * - [CacheKey]
 */
typealias RecordValue = Any?