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
 * Each call to [merge] will merge the given chunk into the [merged] Map, and will also update the [mergedFragmentIds] Set with the
 * value of its `path` and `label` field.
 *
 * The fields in `data` are merged into the node found in [merged] at `path` (for the first call to [merge], the payload is
 * copied to [merged] as-is).
 *
 * `errors` in incremental items (if present) are merged together in an array and then set to the `errors` field of the [merged] Map,
 * at each call to [merge].
 * `extensions` in incremental items (if present) are merged together in an array and then set to the `extensions/incremental` field of the
 * [merged] Map, at each call to [merge].
 */
@ApolloInternal
class DeferredJsonMerger {
  private val _merged: MutableJsonMap = mutableMapOf()
  val merged: JsonMap = _merged

  private val _mergedFragmentIds = mutableSetOf<DeferredFragmentIdentifier>()
  val mergedFragmentIds: Set<DeferredFragmentIdentifier> = _mergedFragmentIds

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

  @Suppress("UNCHECKED_CAST")
  fun merge(payload: JsonMap): JsonMap {
    if (merged.isEmpty()) {
      // Initial payload, no merging needed
      _merged += payload
      return merged
    }

    val incrementalList = payload["incremental"] as? List<JsonMap>
    if (incrementalList == null) {
      isEmptyPayload = true
    } else {
      isEmptyPayload = false
      val mergedErrors = mutableListOf<JsonMap>()
      val mergedExtensions = mutableListOf<JsonMap>()
      for (incrementalItem in incrementalList) {
        mergeData(incrementalItem)
        // Merge errors and extensions (if any) of the incremental list
        (incrementalItem["errors"] as? List<JsonMap>)?.let { mergedErrors += it }
        (incrementalItem["extensions"] as? JsonMap)?.let { mergedExtensions += it }
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

    hasNext = payload["hasNext"] as Boolean? ?: false

    return merged
  }

  @Suppress("UNCHECKED_CAST")
  private fun mergeData(incrementalItem: JsonMap) {
    val data = incrementalItem["data"] as JsonMap?
    val path = incrementalItem["path"] as List<Any>
    val mergedData = merged["data"] as JsonMap

    // payloadData can be null if there are errors
    if (data != null) {
      val nodeToMergeInto = nodeAtPath(mergedData, path) as MutableJsonMap
      deepMerge(nodeToMergeInto, data)

      _mergedFragmentIds += DeferredFragmentIdentifier(path = path, label = incrementalItem["label"] as String?)
    }
  }

  @Suppress("UNCHECKED_CAST")
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

  @Suppress("UNCHECKED_CAST")
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
        @Suppress("UNCHECKED_CAST")
        node as JsonMap
        node[key]
      }
    }
    return node
  }

  fun reset() {
    _merged.clear()
    _mergedFragmentIds.clear()
    hasNext = true
    isEmptyPayload = false
  }
}

internal fun JsonMap.isDeferred(): Boolean {
  return keys.contains("hasNext")
}
