package com.apollographql.apollo3.internal

import com.apollographql.apollo3.annotations.ApolloInternal
import com.apollographql.apollo3.api.json.BufferedSourceJsonReader
import com.apollographql.apollo3.api.json.readAny
import okio.BufferedSource

/**
 * Utility class for merging GraphQL JSON payloads received in multiple chunks when using the `@defer` directive.
 *
 * Each call to [merge] will merge the given chunk into the [merged] Map, and will also update the [mergedFragmentLabels] Set with the
 * value of its `label` field (if present).
 *
 * The fields in `data` are merged into the node found in [merged] at `path` (for the first call to [merge], the payload is
 * copied to [merged] as-is).
 *
 * Errors are merged by adding them to [merged]'s `errors` list field.
 *
 * Extensions are merged by adding them into [merged]'s `extensions` field - if several payloads have the same key in `extensions`,
 * their values are stored as a list.
 */
@ApolloInternal
class DeferredJsonMerger {
  val merged: Map<String, Any?> = mutableMapOf()

  private val _mergedFragmentLabels = mutableSetOf<String>()
  val mergedFragmentLabels: Set<String> = _mergedFragmentLabels

  fun merge(payload: BufferedSource): Map<String, Any?> {
    val payloadMap = jsonToMap(payload)
    if (merged.isEmpty()) {
      // Initial payload, no merging needed
      (merged as MutableMap) += payloadMap
      return merged
    }

    mergeData(payloadMap)
    mergeErrors(payloadMap)
    mergeExtensions(payloadMap)

    return merged
  }

  @Suppress("UNCHECKED_CAST")
  private fun mergeData(payloadMap: Map<String, Any?>) {
    val payloadData = payloadMap["data"] as Map<String, Any?>?
    val payloadPath = payloadMap["path"] as List<Any>
    val mergedData = merged["data"] as Map<String, Any?>

    // payloadData can be null if there are errors
    if (payloadData != null) {
      val nodeToMergeInto = nodeAtPath(mergedData, payloadPath) as MutableMap<String, Any?>
      nodeToMergeInto += payloadData

      (payloadMap["label"] as String?)?.let { _mergedFragmentLabels += it }
    }
  }

  @Suppress("UNCHECKED_CAST")
  private fun mergeErrors(payloadMap: Map<String, Any?>) {
    @Suppress("UNCHECKED_CAST")
    val payloadErrors = payloadMap["errors"] as List<Map<String, Any?>>?
    if (payloadErrors != null) {
      val mergedErrors = (merged as MutableMap<String, Any?>).getOrPut("errors") { mutableListOf<Map<String, Any?>>() } as MutableList<Map<String, Any?>>
      mergedErrors += payloadErrors
    }
  }

  @Suppress("UNCHECKED_CAST")
  private fun mergeExtensions(payloadMap: Map<String, Any?>) {
    val payloadExtensions = payloadMap["extensions"] as Map<String, Any?>?
    if (payloadExtensions != null) {
      val mergedExtensions = (merged as MutableMap<String, Any?>).getOrPut("extensions") { mutableMapOf<String, Any?>() } as MutableMap<String, Any?>
      for ((key, value) in payloadExtensions.entries) {
        if (mergedExtensions.containsKey(key)) {
          // Key already exists in merged extensions: merge the values into a list
          val mergedValue = mergedExtensions[key]
          val mergedValueAsList = mergedValue.asMutableList()
          mergedValueAsList.add(value)
          mergedExtensions[key] = mergedValueAsList
        } else {
          mergedExtensions[key] = value
        }
      }
    }
  }

  @Suppress("UNCHECKED_CAST")
  private fun Any?.asMutableList(): MutableList<Any?> = if (this is MutableList<*>) this as MutableList<Any?> else mutableListOf(this)

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
