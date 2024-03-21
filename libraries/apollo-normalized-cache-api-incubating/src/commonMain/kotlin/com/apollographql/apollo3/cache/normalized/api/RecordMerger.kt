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
    val date = existing.dates?.toMutableMap() ?: mutableMapOf()

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
    val date = existing.dates?.toMutableMap() ?: mutableMapOf()

    for ((fieldKey, incomingFieldValue) in incoming.fields) {
      val hasExistingFieldValue = existing.fields.containsKey(fieldKey)
      val existingFieldValue = existing.fields[fieldKey]
      if (!hasExistingFieldValue) {
        mergedFields[fieldKey] = incomingFieldValue
        mergedMetadata[fieldKey] = incoming.metadata[fieldKey].orEmpty()
        changedKeys.add("${existing.key}.$fieldKey")
      } else if (existingFieldValue != incomingFieldValue) {
        val existingFieldInfo = FieldInfo(
            value = existingFieldValue,
            metadata = existing.metadata[fieldKey].orEmpty(),
        )
        val incomingFieldInfo = FieldInfo(
            value = incomingFieldValue,
            metadata = incoming.metadata[fieldKey].orEmpty(),
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
    } else if (existingStartCursor == null && existingEndCursor == null) {
      // Existing is empty
      incoming
    } else if (incomingStartCursor == null && incomingEndCursor == null) {
      // Incoming is empty
      existing
    } else {
      val existingValue = existing.value as Map<String, Any?>
      val existingEdges = existingValue["edges"] as? List<*>
      val existingNodes = existingValue["nodes"] as? List<*>
      val existingPageInfo = existingValue["pageInfo"] as? Map<String, Any?>
      val existingHasPreviousPage = existingPageInfo?.get("hasPreviousPage") as? Boolean
      val existingHasNextPage = existingPageInfo?.get("hasNextPage") as? Boolean

      val incomingValue = incoming.value as Map<String, Any?>
      val incomingEdges = incomingValue["edges"] as? List<*>
      val incomingNodes = incomingValue["nodes"] as? List<*>
      val incomingPageInfo = incomingValue["pageInfo"] as? Map<String, Any?>
      val incomingHasPreviousPage = incomingPageInfo?.get("hasPreviousPage") as? Boolean
      val incomingHasNextPage = incomingPageInfo?.get("hasNextPage") as? Boolean

      val mergedEdges: List<*>?
      val mergedNodes: List<*>?
      val mergedStartCursor: String?
      val mergedEndCursor: String?
      val mergedHasPreviousPage: Boolean?
      val mergedHasNextPage: Boolean?
      if (incomingAfterArgument == existingEndCursor) {
        // Append to the end
        mergedStartCursor = existingStartCursor
        mergedEndCursor = incomingEndCursor
        mergedEdges = if (existingEdges == null || incomingEdges == null) {
          null
        } else {
          existingEdges + incomingEdges
        }
        mergedNodes = if (existingNodes == null || incomingNodes == null) {
          null
        } else {
          existingNodes + incomingNodes
        }
        mergedHasPreviousPage = existingHasPreviousPage
        mergedHasNextPage = incomingHasNextPage
      } else if (incomingBeforeArgument == existingStartCursor) {
        // Prepend to the start
        mergedStartCursor = incomingStartCursor
        mergedEndCursor = existingEndCursor
        mergedEdges = if (existingEdges == null || incomingEdges == null) {
          null
        } else {
          incomingEdges + existingEdges
        }
        mergedNodes = if (existingNodes == null || incomingNodes == null) {
          null
        } else {
          incomingNodes + existingNodes
        }
        mergedHasPreviousPage = incomingHasPreviousPage
        mergedHasNextPage = existingHasNextPage
      } else {
        // We received a list which is neither the previous nor the next page.
        // Handle this case by resetting the cache with this page
        mergedStartCursor = incomingStartCursor
        mergedEndCursor = incomingEndCursor
        mergedEdges = incomingEdges
        mergedNodes = incomingNodes
        mergedHasPreviousPage = incomingHasPreviousPage
        mergedHasNextPage = incomingHasNextPage
      }

      val mergedPageInfo: Map<String, Any?>? = if (existingPageInfo == null && incomingPageInfo == null) {
        null
      } else {
        (existingPageInfo.orEmpty() + incomingPageInfo.orEmpty()).toMutableMap().also { mergedPageInfo ->
          if (mergedHasNextPage != null) mergedPageInfo["hasNextPage"] = mergedHasNextPage
          if (mergedHasPreviousPage != null) mergedPageInfo["hasPreviousPage"] = mergedHasPreviousPage
          if (mergedStartCursor != null) mergedPageInfo["startCursor"] = mergedStartCursor
          if (mergedEndCursor != null) mergedPageInfo["endCursor"] = mergedEndCursor
        }
      }

      val mergedValue = (existingValue + incomingValue).toMutableMap()
      if (mergedEdges != null) mergedValue["edges"] = mergedEdges
      if (mergedNodes != null) mergedValue["nodes"] = mergedNodes
      if (mergedPageInfo != null) mergedValue["pageInfo"] = mergedPageInfo

      FieldRecordMerger.FieldInfo(
          value = mergedValue,
          metadata = mapOf("startCursor" to mergedStartCursor, "endCursor" to mergedEndCursor)
      )
    }
  }
}
