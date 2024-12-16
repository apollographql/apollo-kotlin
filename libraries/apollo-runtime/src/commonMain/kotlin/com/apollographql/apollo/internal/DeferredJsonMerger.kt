package com.apollographql.apollo.internal

import com.apollographql.apollo.annotations.ApolloInternal
import com.apollographql.apollo.api.DeferredFragmentIdentifier
import com.apollographql.apollo.api.json.BufferedSourceJsonReader
import com.apollographql.apollo.api.json.readAny
import okio.BufferedSource

private typealias JsonMap = Map<String, Any?>
private typealias MutableJsonMap = MutableMap<String, Any?>

/**
 * Utility class for merging GraphQL JSON payloads received in multiple chunks when using the `@defer` directive.
 *
 * Each call to [merge] will merge the given chunk into the [merged] Map, and will also update the [pendingFragmentIds] Set with the
 * value of its `path` and `label` field.
 *
 * The fields in `data` are merged into the node found in [merged] at the path known by looking at the `id` field (for the first call to
 * [merge], the payload is copied to [merged] as-is).
 *
 * `errors` in incremental and completed items (if present) are merged together in an array and then set to the `errors` field of the
 * [merged] Map, at each call to [merge].
 * `extensions` in incremental items (if present) are merged together in an array and then set to the `extensions` field of the [merged]
 * Map, at each call to [merge].
 */
@ApolloInternal
@Suppress("UNCHECKED_CAST")
class DeferredJsonMerger {
  private val _merged: MutableJsonMap = mutableMapOf()
  val merged: JsonMap = _merged

  /**
   * Map of identifiers to their corresponding DeferredFragmentIdentifier, found in `pending`.
   */
  private val _pendingFragmentIds = mutableMapOf<String, DeferredFragmentIdentifier>()
  val pendingFragmentIds: Set<DeferredFragmentIdentifier> get() = _pendingFragmentIds.values.toSet()

  var hasNext: Boolean = true
    private set

  /**
   * A payload can sometimes have no `incremental` field, e.g. when the server couldn't predict if there were more data after the last
   * emitted payload. This field allows to test for this in order to ignore such payloads.
   * See https://github.com/apollographql/router/issues/1687.
   */
  var isEmptyPayload: Boolean = false
    private set

  fun merge(payload: BufferedSource): JsonMap {
    val payloadMap = jsonToMap(payload)
    return merge(payloadMap)
  }

  fun merge(payload: JsonMap): JsonMap {
    val completed = payload["completed"] as? List<JsonMap>
    if (merged.isEmpty()) {
      // Initial payload, no merging needed (strip some fields that should not appear in the final result)
      _merged += payload - "hasNext" - "pending"
      handlePending(payload)
      handleCompleted(completed)
      return merged
    }
    handlePending(payload)

    val incrementalList = payload["incremental"] as? List<JsonMap>
    if (incrementalList != null) {
      for (incrementalItem in incrementalList) {
        mergeIncrementalData(incrementalItem)
        // Merge errors (if any) of the incremental item
        (incrementalItem["errors"] as? List<JsonMap>)?.let { getOrPutMergedErrors() += it }
      }
    }
    isEmptyPayload = completed == null && incrementalList == null

    hasNext = payload["hasNext"] as Boolean? ?: false

    handleCompleted(completed)

    (payload["extensions"] as? JsonMap)?.let { getOrPutExtensions() += it }

    return merged
  }

  private fun getOrPutMergedErrors() = _merged.getOrPut("errors") { mutableListOf<JsonMap>() } as MutableList<JsonMap>

  private fun getOrPutExtensions() = _merged.getOrPut("extensions") { mutableMapOf<String, Any?>() } as MutableJsonMap

  private fun handlePending(payload: JsonMap) {
    val pending = payload["pending"] as? List<JsonMap>
    if (pending != null) {
      for (pendingItem in pending) {
        val id = pendingItem["id"] as String
        val path = pendingItem["path"] as List<Any>
        val label = pendingItem["label"] as String?
        _pendingFragmentIds[id] = DeferredFragmentIdentifier(path = path, label = label)
      }
    }
  }

  private fun handleCompleted(completed: List<JsonMap>?) {
    if (completed != null) {
      for (completedItem in completed) {
        // Merge errors (if any) of the completed item
        val errors = completedItem["errors"] as? List<JsonMap>
        if (errors != null) {
          getOrPutMergedErrors() += errors
        } else {
          // Fragment is no longer pending - only if there were no errors
          val id = completedItem["id"] as String
          _pendingFragmentIds.remove(id) ?: error("Id '$id' not found in pending results")
        }
      }
    }
  }

  private fun mergeIncrementalData(incrementalItem: JsonMap) {
    val id = incrementalItem["id"] as String? ?: error("No id found in incremental item")
    val data = incrementalItem["data"] as JsonMap? ?: error("No data found in incremental item")
    val subPath = incrementalItem["subPath"] as List<Any>? ?: emptyList()
    val path = (_pendingFragmentIds[id]?.path ?: error("Id '$id' not found in pending results")) + subPath
    val mergedData = merged["data"] as JsonMap
    val nodeToMergeInto = nodeAtPath(mergedData, path) as MutableJsonMap
    deepMerge(nodeToMergeInto, data)
  }

  private fun deepMerge(destination: MutableJsonMap, map: JsonMap) {
    for ((key, value) in map) {
      if (destination.containsKey(key) && destination[key] is MutableMap<*, *>) {
        // Objects: merge recursively
        val fieldDestination = destination[key] as MutableJsonMap
        val fieldMap = value as? JsonMap ?: error("'$key' is an object in destination but not in map")
        deepMerge(destination = fieldDestination, map = fieldMap)
      } else {
        // Other types: add / overwrite
        destination[key] = value
      }
    }
  }

  private fun jsonToMap(json: BufferedSource): JsonMap = BufferedSourceJsonReader(json).readAny() as JsonMap


  /**
   * Find the node in the [map] at the given [path].
   * @param path The path to the node to find, as a list of either `String` (name of field in object) or `Int` (index of element in array).
   */
  private fun nodeAtPath(map: JsonMap, path: List<Any>): Any? {
    var node: Any? = map
    for (key in path) {
      node = if (node is List<*>) {
        node[key as Int]
      } else {
        node as JsonMap
        node[key]
      }
    }
    return node
  }

  fun reset() {
    _merged.clear()
    _pendingFragmentIds.clear()
    hasNext = true
    isEmptyPayload = false
  }
}

internal fun JsonMap.isDeferred(): Boolean {
  return keys.contains("hasNext")
}
