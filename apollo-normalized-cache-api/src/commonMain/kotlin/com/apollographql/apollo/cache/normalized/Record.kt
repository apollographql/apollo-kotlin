package com.apollographql.apollo.cache.normalized

import com.apollographql.apollo.cache.normalized.internal.RecordWeigher.byteChange
import com.apollographql.apollo.cache.normalized.internal.RecordWeigher.calculateBytes
import com.benasher44.uuid.Uuid
import kotlin.jvm.JvmStatic
import kotlin.jvm.Synchronized
import kotlin.jvm.Volatile

/**
 * A normalized entry that corresponds to a response object. Object fields are stored if they are a GraphQL Scalars. If
 * a field is a GraphQL Object a [CacheReference] will be stored instead.
 */
class Record internal constructor(
    val key: String,
    private val _fields: MutableMap<String, Any?>,
    mutationId: Uuid?
): Map<String, Any?> by _fields {

  val fields: Map<String, Any?> get() = _fields

  @field:Volatile
  var mutationId = mutationId
    private set

  private var sizeInBytes = UNKNOWN_SIZE_ESTIMATE

  fun toBuilder(): Builder = Builder(key, fields, mutationId)

  fun field(fieldKey: String): Any? = fields[fieldKey]

  fun hasField(fieldKey: String): Boolean = fields.containsKey(fieldKey)

  override fun toString(): String {
    return "Record(key='$key', fields=$fields, mutationId=$mutationId)"
  }

  /**
   * @param otherRecord The record to merge into this record.
   * @return A set of field keys which have changed, or were added. A field key incorporates any GraphQL arguments in
   * addition to the field name.
   */
  fun mergeWith(otherRecord: Record): Set<String> {
    val changedKeys = mutableSetOf<String>()
    for ((otherKey, newFieldValue) in otherRecord.fields) {
      val hasOldFieldValue = fields.containsKey(otherKey)
      val oldFieldValue = fields[otherKey]
      if (!hasOldFieldValue || oldFieldValue != newFieldValue) {
        _fields[otherKey] = newFieldValue
        changedKeys.add("$key.$otherKey")
        adjustSizeEstimate(newFieldValue, oldFieldValue)
      }
    }
    mutationId = otherRecord.mutationId
    return changedKeys
  }

  /**
   * @return A set of all field keys. A field key incorporates any GraphQL arguments in addition to the field name.
   */
  fun keys(): Set<String> {
    return fields.keys.map { "$key.$it" }.toSet()
  }

  /**
   * Returns the list of referenced cache fields
   *
   * @return the list of referenced cache fields
   */
  fun referencedFields(): List<CacheReference> {
    val cacheReferences = mutableListOf<CacheReference>()
    for (value in fields.values) {
      findCacheReferences(value, cacheReferences)
    }
    return cacheReferences
  }

  /**
   * @return An approximate number of bytes this Record takes up.
   */
  @Synchronized
  fun sizeEstimateBytes(): Int {
    if (sizeInBytes == UNKNOWN_SIZE_ESTIMATE) {
      sizeInBytes = calculateBytes(this)
    }
    return sizeInBytes
  }

  @Synchronized
  private fun adjustSizeEstimate(newFieldValue: Any?, oldFieldValue: Any?) {
    if (sizeInBytes != UNKNOWN_SIZE_ESTIMATE) {
      sizeInBytes += byteChange(newFieldValue, oldFieldValue)
    }
  }

  class Builder(
      val key: String,
      fields: Map<String, Any?>,
      private var mutationId: Uuid?
  ) {

    private val fields: MutableMap<String, Any?> = LinkedHashMap(fields)

    fun addField(key: String, value: Any?): Builder {
      fields[key] = value
      return this
    }

    fun addFields(fields: Map<String, Any?>): Builder {
      this.fields.putAll(fields)
      return this
    }

    fun mutationId(mutationId: Uuid?): Builder {
      this.mutationId = mutationId
      return this
    }

    fun build(): Record {
      return Record(key, fields, mutationId)
    }
  }

  companion object {
    private const val UNKNOWN_SIZE_ESTIMATE = -1

    @JvmStatic
    fun builder(key: String): Builder {
      return Builder(key, LinkedHashMap(), null)
    }

    private fun findCacheReferences(cachedValue: Any?, result: MutableList<CacheReference>) {
      when (cachedValue) {
        is CacheReference -> result.add(cachedValue)
        is Map<*, *> -> cachedValue.values.forEach { findCacheReferences(it, result) }
        is List<*> -> cachedValue.forEach { findCacheReferences(it, result) }
      }
    }
  }

}
