package com.apollographql.apollo3.cache.normalized

import com.apollographql.apollo3.cache.normalized.internal.RecordWeigher.calculateBytes
import com.benasher44.uuid.Uuid

/**
 * A normalized entry that corresponds to a response object. Object fields are stored if they are a GraphQL Scalars. If
 * a field is a GraphQL Object a [CacheReference] will be stored instead.
 */
class Record (
    val key: String,
    /**
     * a list of fields. Values can be
     * - Int
     * - Long
     * - Boolean
     * - String
     * - Double
     * - CacheReference
     * - List
     * - Map (for custom scalars)
     * - null
     */
    val fields: Map<String, Any?>,
    val mutationId: Uuid? = null,
) : Map<String, Any?> by fields {

  val sizeInBytes = calculateBytes(this)

  /**
   * Returns a merge result record and a set of field keys which have changed, or were added.
   * A field key incorporates any GraphQL arguments in addition to the field name.
   */
  fun mergeWith(otherRecord: Record): Pair<Record, Set<String>> {
    val changedKeys = mutableSetOf<String>()
    val mergedFields = fields.toMutableMap()

    for ((otherKey, newFieldValue) in otherRecord.fields) {
      val hasOldFieldValue = fields.containsKey(otherKey)
      val oldFieldValue = fields[otherKey]
      if (!hasOldFieldValue || oldFieldValue != newFieldValue) {
        mergedFields[otherKey] = newFieldValue
        changedKeys.add("$key.$otherKey")
      }
    }

    return Record(
        key = key,
        fields = mergedFields,
        mutationId = otherRecord.mutationId
    ) to changedKeys
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
  fun referencedFields(): List<CacheReference> {
    val result = mutableListOf<CacheReference>()
    val stack = fields.values.toMutableList()
    while (stack.isNotEmpty()) {
      when (val value = stack.removeAt(stack.size - 1)) {
        is CacheReference -> result.add(value)
        is Map<*, *> -> stack.addAll(value.values)
        is List<*> -> stack.addAll(value)
      }
    }
    return result
  }
}
