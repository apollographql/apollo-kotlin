package com.apollographql.apollo.internal.incremental

import com.apollographql.apollo.api.DeferredFragmentIdentifier
import okio.BufferedSource

/**
 * Merger for the [com.apollographql.apollo.network.http.HttpNetworkTransport.IncrementalDeliveryProtocol.GraphQL17Alpha9] protocol format.
 */
@Suppress("UNCHECKED_CAST")
internal class GraphQL17Alpha9IncrementalResultsMerger : IncrementalResultsMerger {
  private val _merged: MutableJsonMap = mutableMapOf()
  override val merged: JsonMap = _merged

  /**
   * Map of identifiers to their corresponding IncrementalResultIdentifier, found in `pending`.
   */
  private val _pendingResultIds = mutableMapOf<String, DeferredFragmentIdentifier>()

  /**
   * For this protocol, this represents the set of ids that are pending.
   */
  override val deferredFragmentIdentifiers: Set<DeferredFragmentIdentifier> get() = _pendingResultIds.values.toSet() + DeferredFragmentIdentifier.Pending

  override var hasNext: Boolean = true
    private set

  override var isEmptyResponse: Boolean = false
    private set

  override fun merge(part: BufferedSource): JsonMap {
    return merge(part.toJsonMap())
  }

  override fun merge(part: JsonMap): JsonMap {
    val completed = part["completed"] as? List<JsonMap>
    if (merged.isEmpty()) {
      // Initial part, no merging needed (strip some fields that should not appear in the final result)
      _merged += part - "hasNext" - "pending"
      handlePending(part)
      handleCompleted(completed)
      return merged
    }
    handlePending(part)

    val incremental = part["incremental"] as? List<JsonMap>
    if (incremental != null) {
      for (incrementalResult in incremental) {
        mergeIncrementalResult(incrementalResult)
        // Merge errors (if any) of the incremental result
        (incrementalResult["errors"] as? List<JsonMap>)?.let { getOrPutMergedErrors() += it }
      }
    }
    isEmptyResponse = completed == null && incremental == null

    hasNext = part["hasNext"] as Boolean? ?: false

    handleCompleted(completed)

    (part["extensions"] as? JsonMap)?.let { getOrPutExtensions() += it }

    return merged
  }

  private fun getOrPutMergedErrors() = _merged.getOrPut("errors") { mutableListOf<JsonMap>() } as MutableList<JsonMap>

  private fun getOrPutExtensions() = _merged.getOrPut("extensions") { mutableMapOf<String, Any?>() } as MutableJsonMap

  private fun handlePending(part: JsonMap) {
    val pending = part["pending"] as? List<JsonMap>
    if (pending != null) {
      for (pendingResult in pending) {
        val id = pendingResult["id"] as String
        val path = pendingResult["path"] as List<Any>
        val label = pendingResult["label"] as String?
        _pendingResultIds[id] = DeferredFragmentIdentifier(path = path, label = label)
      }
    }
  }

  private fun handleCompleted(completed: List<JsonMap>?) {
    if (completed != null) {
      for (completedResult in completed) {
        // Merge errors (if any) of the completed result
        val errors = completedResult["errors"] as? List<JsonMap>
        if (errors != null) {
          getOrPutMergedErrors() += errors
        } else {
          // Fragment is no longer pending - only if there were no errors
          val id = completedResult["id"] as String
          _pendingResultIds.remove(id) ?: error("Id '$id' not found in pending results")
        }
      }
    }
  }

  private fun mergeIncrementalResult(incrementalResult: JsonMap) {
    val id = incrementalResult["id"] as String? ?: error("No id found in incremental result")
    val data = incrementalResult["data"] as JsonMap?
    val items = incrementalResult["items"] as List<Any>?
    val subPath = incrementalResult["subPath"] as List<Any>? ?: emptyList()
    val path = (_pendingResultIds[id]?.path ?: error("Id '$id' not found in pending results")) + subPath
    val mergedData = merged["data"] as JsonMap
    val nodeToMergeInto = nodeAtPath(mergedData, path)
    when {
      data != null -> {
        deepMergeObject(nodeToMergeInto as MutableJsonMap, data)
      }

      items != null -> {
        mergeList(nodeToMergeInto as MutableList<Any>, items)
      }

      else -> {
        error("Neither data nor items found in incremental result")
      }
    }
  }

  private fun mergeList(destination: MutableList<Any>, items: List<Any>) {
    destination.addAll(items)
  }

  override fun reset() {
    _merged.clear()
    _pendingResultIds.clear()
    hasNext = true
    isEmptyResponse = false
  }
}
