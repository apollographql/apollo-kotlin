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
      if (!hasExistingFieldValue) {
        mergedFields[fieldKey] = incomingFieldValue
        mergedMetadata[fieldKey] = incoming.metadata[fieldKey]!!
        changedKeys.add("${existing.key}.$fieldKey")
      } else if (existingFieldValue != incomingFieldValue) {
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

@ApolloExperimental
val ConnectionRecordMerger = FieldRecordMerger(ConnectionFieldMerger)

private object ConnectionFieldMerger : FieldRecordMerger.FieldMerger {
  @Suppress("UNCHECKED_CAST")
  override fun mergeFields(existing: FieldRecordMerger.FieldInfo, incoming: FieldRecordMerger.FieldInfo): FieldRecordMerger.FieldInfo {
    val existingStartCursor = existing.metadata["startCursor"] as? String
    val existingEndCursor = existing.metadata["endCursor"] as? String
    val incomingStartCursor = incoming.metadata["startCursor"] as? String
    val incomingEndCursor = incoming.metadata["endCursor"] as? String
    val incomingBeforeArgument = incoming.metadata["before"] as? String
    val incomingAfterArgument = incoming.metadata["after"] as? String

    return if (incomingBeforeArgument == null && incomingAfterArgument == null) {
      // Not a pagination query
      incoming
    } else if (existingStartCursor == null || existingEndCursor == null) {
      // Existing is empty
      incoming
    } else if (incomingStartCursor == null || incomingEndCursor == null) {
      // Incoming is empty
      existing
    } else {
      val existingValue = existing.value as Map<String, Any?>
      val existingList = existingValue["edges"] as List<*>
      val incomingList = (incoming.value as Map<String, Any?>)["edges"] as List<*>

      val mergedList: List<*>
      val newStartCursor: String
      val newEndCursor: String
      if (incomingAfterArgument == existingEndCursor) {
        mergedList = existingList + incomingList
        newStartCursor = existingStartCursor
        newEndCursor = incomingEndCursor
      } else if (incomingBeforeArgument == existingStartCursor) {
        mergedList = incomingList + existingList
        newStartCursor = incomingStartCursor
        newEndCursor = existingEndCursor
      } else {
        // We received a list which is neither the previous nor the next page.
        // Handle this case by resetting the cache with this page
        mergedList = incomingList
        newStartCursor = incomingStartCursor
        newEndCursor = incomingEndCursor
      }

      val mergedFieldValue = existingValue.toMutableMap()
      mergedFieldValue["edges"] = mergedList
      FieldRecordMerger.FieldInfo(
          value = mergedFieldValue,
          metadata = mapOf("startCursor" to newStartCursor, "endCursor" to newEndCursor)
      )
    }
  }
}
