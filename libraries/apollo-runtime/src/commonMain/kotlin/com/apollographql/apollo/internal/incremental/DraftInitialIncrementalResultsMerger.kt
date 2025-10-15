package com.apollographql.apollo.internal.incremental

import com.apollographql.apollo.api.DeferredFragmentIdentifier
import okio.BufferedSource

/**
 * Merger for the [com.apollographql.apollo.network.IncrementalDeliveryProtocol.DraftInitial] protocol format.
 */
@Suppress("UNCHECKED_CAST")
internal class DraftInitialIncrementalResultsMerger : IncrementalResultsMerger {
  private val _merged: MutableJsonMap = mutableMapOf()
  override val merged: JsonMap = _merged

  private val _deferredFragmentIdentifiers = mutableSetOf<DeferredFragmentIdentifier>()

  /**
   * For this protocol, this represents the set of fragment ids that are already merged.
   */
  override val deferredFragmentIdentifiers: Set<DeferredFragmentIdentifier> = _deferredFragmentIdentifiers

  override var hasNext: Boolean = true
    private set

  override var isEmptyResponse: Boolean = false
    private set

  override fun merge(part: BufferedSource): JsonMap {
    return merge(part.toJsonMap())
  }

  override fun merge(part: JsonMap): JsonMap {
    if (merged.isEmpty()) {
      // Initial part, no merging needed
      _merged += part
      return merged
    }

    val incremental = part["incremental"] as? List<JsonMap>
    if (incremental == null) {
      isEmptyResponse = true
    } else {
      isEmptyResponse = false
      val mergedErrors = mutableListOf<JsonMap>()
      val mergedExtensions = mutableListOf<JsonMap>()
      for (incrementalResult in incremental) {
        incrementalResult(incrementalResult)
        // Merge errors and extensions (if any) of the incremental result
        (incrementalResult["errors"] as? List<JsonMap>)?.let { mergedErrors += it }
        (incrementalResult["extensions"] as? JsonMap)?.let { mergedExtensions += it }
      }
      // Keep only this payload's errors and extensions, if any
      if (mergedErrors.isNotEmpty()) {
        _merged["errors"] = mergedErrors
      } else {
        _merged.remove("errors")
      }
      if (mergedExtensions.isNotEmpty()) {
        _merged["extensions"] = mapOf("incremental" to mergedExtensions)
      } else {
        _merged.remove("extensions")
      }
    }

    hasNext = part["hasNext"] as Boolean? ?: false

    return merged
  }

  private fun incrementalResult(incrementalResult: JsonMap) {
    val data = incrementalResult["data"] as JsonMap?
    val path = incrementalResult["path"] as List<Any>
    val mergedData = merged["data"] as JsonMap

    // data can be null if there are errors
    if (data != null) {
      val nodeToMergeInto = nodeAtPath(mergedData, path) as MutableJsonMap
      deepMergeObject(nodeToMergeInto, data)

      _deferredFragmentIdentifiers += DeferredFragmentIdentifier(path = path, label = incrementalResult["label"] as String?)
    }
  }

  override fun reset() {
    _merged.clear()
    _deferredFragmentIdentifiers.clear()
    hasNext = true
    isEmptyResponse = false
  }
}
