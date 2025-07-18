package com.apollographql.apollo.internal

import com.apollographql.apollo.annotations.ApolloInternal
import com.apollographql.apollo.api.IncrementalResultIdentifier
import com.apollographql.apollo.api.json.BufferedSourceJsonReader
import com.apollographql.apollo.api.json.readAny
import okio.BufferedSource

private typealias JsonMap = Map<String, Any?>
private typealias MutableJsonMap = MutableMap<String, Any?>

/**
 * Utility class for merging GraphQL incremental results received in multiple chunks when using the `@defer` and/or `@stream` directives.
 *
 * Each call to [merge] will merge the given results into the [merged] Map, and will also update the [pendingResultIds] Set with the
 * value of their `path` and `label` fields.
 *
 * The fields in `data` are merged into the node found in [merged] at the path known by looking at the `id` field. For the first call to
 * [merge], the payload is copied to [merged] as-is.
 *
 * `errors` in incremental and completed results (if present) are merged together in an array and then set to the `errors` field of the
 * [merged] Map.
 * `extensions` in incremental results (if present) are merged together in an array and then set to the `extensions` field of the [merged]
 * Map.
 */
@ApolloInternal
@Suppress("UNCHECKED_CAST")
class IncrementalResultsMerger {
  private val _merged: MutableJsonMap = mutableMapOf()
  val merged: JsonMap = _merged

  /**
   * Map of identifiers to their corresponding IncrementalResultIdentifier, found in `pending`.
   */
  private val _pendingResultIds = mutableMapOf<String, IncrementalResultIdentifier>()
  val pendingResultIds: Set<IncrementalResultIdentifier> get() = _pendingResultIds.values.toSet()

  var hasNext: Boolean = true
    private set

  /**
   * A response can sometimes have no `incremental` field, e.g. when the server couldn't predict if there were more data after the last
   * emitted payload. This field allows to test for this in order to ignore such payloads.
   * See https://github.com/apollographql/router/issues/1687.
   */
  var isEmptyResponse: Boolean = false
    private set

  fun merge(part: BufferedSource): JsonMap {
    return merge(part.toJsonMap())
  }

  fun merge(part: JsonMap): JsonMap {
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
        _pendingResultIds[id] = IncrementalResultIdentifier(path = path, label = label)
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

  private fun deepMergeObject(destination: MutableJsonMap, obj: JsonMap) {
    for ((key, value) in obj) {
      if (destination.containsKey(key) && destination[key] is MutableMap<*, *>) {
        // Objects: merge recursively
        val fieldDestination = destination[key] as MutableJsonMap
        val fieldMap = value as? JsonMap ?: error("'$key' is an object in destination but not in map")
        deepMergeObject(destination = fieldDestination, obj = fieldMap)
      } else {
        // Other types: add / overwrite
        destination[key] = value
      }
    }
  }

  private fun mergeList(destination: MutableList<Any>, items: List<Any>) {
    destination.addAll(items)
  }

  private fun BufferedSource.toJsonMap(): JsonMap = BufferedSourceJsonReader(this).readAny() as JsonMap


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
    _pendingResultIds.clear()
    hasNext = true
    isEmptyResponse = false
  }
}

internal fun JsonMap.isIncremental(): Boolean {
  return keys.contains("hasNext")
}
