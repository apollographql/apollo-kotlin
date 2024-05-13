package com.apollographql.apollo3.cache.normalized.api

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.api.json.ApolloJsonElement
import com.apollographql.apollo3.cache.normalized.api.FieldRecordMerger.FieldMerger

/**
 * Used to merge incoming [Record]s from the network with existing ones in the cache.
 */
@ApolloExperimental
interface RecordMerger {
  /**
   * Merges the incoming Record with the existing Record.
   *
   * @param newDate optional date to associate with the fields of the resulting merged Record. If null, a date will not be set.
   * @return a pair of the resulting merged Record and a set of field keys which have changed or were added.
   */
  fun merge(existing: Record, incoming: Record, newDate: Long?): Pair<Record, Set<String>>
}

/**
 * Default [RecordMerger] that merges fields by replacing them with the incoming fields.
 */
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
      // Update the date even if the value did not change
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

/**
 * Convenience implementation of [RecordMerger] that simplifies the merging of [Record]s by delegating to a [FieldMerger].
 */
@ApolloExperimental
class FieldRecordMerger(private val fieldMerger: FieldMerger) : RecordMerger {
  /**
   * Used to merge Records field by field.
   */
  @ApolloExperimental
  interface FieldMerger {
    /**
     * Merges the existing field with the incoming field.
     *
     * @return the merged field and its metadata.
     */
    fun mergeFields(existing: FieldInfo, incoming: FieldInfo): FieldInfo
  }

  @ApolloExperimental
  data class FieldInfo(
      /**
       * Value of the field being merged.
       */
      val value: ApolloJsonElement,

      /**
       * Metadata attached to the field being merged. See also [Record.metadata] and [MetadataGenerator].
       */
      val metadata: Map<String, ApolloJsonElement>,
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
      // Update the date even if the value did not change
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

/**
 * A [RecordMerger] that merges lists in [Relay connection types](https://relay.dev/graphql/connections.htm#sec-Connection-Types).
 *
 * It will merge the `edges` and `nodes` lists and update the `pageInfo` field.
 *
 * If the incoming data can't be merged with the existing data, it will replace the existing data.
 *
 * Note: although `nodes` is not a standard field in Relay, it is often used - see
 * [this issue on the Relay spec](https://github.com/facebook/relay/issues/3850) that discusses this pattern.
 */
@ApolloExperimental
val ConnectionRecordMerger = FieldRecordMerger(ConnectionFieldMerger)

private object ConnectionFieldMerger : FieldMerger {
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
      val existingValue = existing.value as Map<String, ApolloJsonElement>
      val existingEdges = existingValue["edges"] as? List<*>
      val existingNodes = existingValue["nodes"] as? List<*>
      val existingPageInfo = existingValue["pageInfo"] as? Map<String, ApolloJsonElement>
      val existingHasPreviousPage = existingPageInfo?.get("hasPreviousPage") as? Boolean
      val existingHasNextPage = existingPageInfo?.get("hasNextPage") as? Boolean

      val incomingValue = incoming.value as Map<String, ApolloJsonElement>
      val incomingEdges = incomingValue["edges"] as? List<*>
      val incomingNodes = incomingValue["nodes"] as? List<*>
      val incomingPageInfo = incomingValue["pageInfo"] as? Map<String, ApolloJsonElement>
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

      val mergedPageInfo: Map<String, ApolloJsonElement>? = if (existingPageInfo == null && incomingPageInfo == null) {
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
