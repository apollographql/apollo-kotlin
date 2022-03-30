package com.apollographql.apollo3.internal

import com.apollographql.apollo3.annotations.ApolloInternal
import com.apollographql.apollo3.api.DeferredFragmentIdentifier
import com.apollographql.apollo3.api.json.BufferedSourceJsonReader
import com.apollographql.apollo3.api.json.readAny
import okio.BufferedSource

/**
 * Utility class for merging GraphQL JSON payloads received in multiple chunks when using the `@defer` directive.
 *
 * Each call to [merge] will merge the given chunk into the [merged] Map, and will also update the [mergedFragmentIds] Set with the
 * value of its `path` and `label` field.
 *
 * The fields in `data` are merged into the node found in [merged] at `path` (for the first call to [merge], the payload is
 * copied to [merged] as-is).
 *
 * `errors` and `extensions` fields are not merged: they are copied as-is (if present) to the [merged] Map at each call to [merge].
 */
@ApolloInternal
class DeferredJsonMerger {
  private val _merged = mutableMapOf<String, Any?>()
  val merged: Map<String, Any?> = _merged

  private val _mergedFragmentIds = mutableSetOf<DeferredFragmentIdentifier>()
  val mergedFragmentIds: Set<DeferredFragmentIdentifier> = _mergedFragmentIds

  var hasNext: Boolean = true
    private set

  fun merge(payload: BufferedSource): Map<String, Any?> {
    val payloadMap = jsonToMap(payload)
    if (merged.isEmpty()) {
      // Initial payload, no merging needed
      _merged += payloadMap
      return merged
    }

    mergeData(payloadMap)
    if (payloadMap.containsKey("errors")) {
      _merged["errors"] = payloadMap["errors"]
    } else {
      _merged.remove("errors")
    }
    if (payloadMap.containsKey("extensions")) {
      _merged["extensions"] = payloadMap["extensions"]
    } else {
      _merged.remove("extensions")
    }

    return merged
  }

  @Suppress("UNCHECKED_CAST")
  private fun mergeData(payloadMap: Map<String, Any?>) {
    val payloadData = payloadMap["data"] as Map<String, Any?>?
    val payloadPath = payloadMap["path"] as List<Any>
    val mergedData = merged["data"] as Map<String, Any?>
    hasNext = payloadMap["hasNext"] as Boolean? ?: false

    // payloadData can be null if there are errors
    if (payloadData != null) {
      val nodeToMergeInto = nodeAtPath(mergedData, payloadPath) as MutableMap<String, Any?>
      deepMerge(nodeToMergeInto, payloadData)

      _mergedFragmentIds += DeferredFragmentIdentifier(path = payloadPath, label = payloadMap["label"] as String?)
    }
  }

  @Suppress("UNCHECKED_CAST")
  private fun deepMerge(destination: MutableMap<String, Any?>, map: Map<String, Any?>) {
    for ((key, value) in map) {
      if (destination.containsKey(key) && destination[key] is MutableMap<*, *>) {
        // Objects: merge recursively
        val fieldDestination = destination[key] as MutableMap<String, Any?>
        val fieldMap = value as? Map<String, Any?> ?: error("'$key' is an object in destination but not in map")
        deepMerge(destination = fieldDestination, map = fieldMap)
      } else {
        // Other types: add / overwrite
        destination[key] = value
      }
    }
  }

  @Suppress("UNCHECKED_CAST")
  private fun jsonToMap(json: BufferedSource): Map<String, Any?> = BufferedSourceJsonReader(json).readAny() as Map<String, Any?>


  /**
   * Find the node in the [map] at the given [path].
   * @param path The path to the node to find, as a list of either `String` (name of field in object) or `Int` (index of element in array).
   */
  private fun nodeAtPath(map: Map<String, Any?>, path: List<Any>): Any? {
    var node: Any? = map
    for (key in path) {
      node = if (node is List<*>) {
        node[key as Int]
      } else {
        @Suppress("UNCHECKED_CAST")
        node as Map<String, Any?>
        node[key]
      }
    }
    return node
  }
}
