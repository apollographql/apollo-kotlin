package com.apollographql.apollo3.cache.normalized.api

import com.apollographql.apollo3.annotations.ApolloExperimental

@ApolloExperimental
interface RecordMerger {
  fun merge(existing: Record, incoming: Record, newDate: Long?): Pair<Record, Set<String>>
}

@ApolloExperimental
object DefaultRecordMerger : RecordMerger {
  override fun merge(existing: Record, incoming: Record, newDate: Long?): Pair<Record, Set<String>> {
    val changedKeys = mutableSetOf<String>()
    val mergedFields = existing.fields.toMutableMap()
    val date = existing.date?.toMutableMap() ?: mutableMapOf()

    for ((fieldKey, incomingFieldValue) in incoming.fields) {
      val hasExistingFieldValue = existing.fields.containsKey(fieldKey)
      val existingFieldValue = existing.fields[fieldKey]
      if (!hasExistingFieldValue || existingFieldValue != incomingFieldValue) {
        mergedFields[fieldKey] = incomingFieldValue
        changedKeys.add("${existing.key}.$fieldKey")
      }
      // Even if the value did not change update date
      if (newDate != null) {
        date[fieldKey] = newDate
      }
    }

    return Record(
        key = existing.key,
        fields = mergedFields,
        mutationId = incoming.mutationId,
        date = date,
        metadata = existing.metadata + incoming.metadata,
    ) to changedKeys
  }
}

@ApolloExperimental
class FieldRecordMerger(private val fieldMerger: FieldMerger) : RecordMerger {
  @ApolloExperimental
  interface FieldMerger {
    fun mergeFields(existing: FieldInfo, incoming: FieldInfo): FieldInfo
  }

  @ApolloExperimental
  data class FieldInfo(
      val value: Any?,
      val metadata: Map<String, Any?>,
  )

  override fun merge(existing: Record, incoming: Record, newDate: Long?): Pair<Record, Set<String>> {
    val changedKeys = mutableSetOf<String>()
    val mergedFields = existing.fields.toMutableMap()
    val mergedMetadata = existing.metadata.toMutableMap()
    val date = existing.date?.toMutableMap() ?: mutableMapOf()

    for ((fieldKey, incomingFieldValue) in incoming.fields) {
      val hasExistingFieldValue = existing.fields.containsKey(fieldKey)
      val existingFieldValue = existing.fields[fieldKey]
      if (!hasExistingFieldValue || existingFieldValue != incomingFieldValue) {
        val existingFieldInfo = FieldInfo(
            value = existingFieldValue,
            metadata = existing.metadata[fieldKey]!!,
        )
        val incomingFieldInfo = FieldInfo(
            value = incomingFieldValue,
            metadata = incoming.metadata[fieldKey]!!,
        )

        val mergeResult = fieldMerger.mergeFields(existing = existingFieldInfo, incoming = incomingFieldInfo)
        mergedFields[fieldKey] = mergeResult.value
        mergedMetadata[fieldKey] = mergeResult.metadata

        changedKeys.add("${existing.key}.$fieldKey")
      }
      // Even if the value did not change update date
      if (newDate != null) {
        date[fieldKey] = newDate
      }
    }

    return Record(
        key = existing.key,
        fields = mergedFields,
        mutationId = incoming.mutationId,
        date = date,
        metadata = mergedMetadata,
    ) to changedKeys
  }
}
