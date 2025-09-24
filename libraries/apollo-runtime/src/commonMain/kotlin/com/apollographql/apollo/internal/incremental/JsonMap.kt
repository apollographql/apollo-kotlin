@file:Suppress("UNCHECKED_CAST")

package com.apollographql.apollo.internal.incremental

import com.apollographql.apollo.api.json.BufferedSourceJsonReader
import com.apollographql.apollo.api.json.readAny
import okio.BufferedSource

internal typealias JsonMap = Map<String, Any?>
internal typealias MutableJsonMap = MutableMap<String, Any?>


/**
 * Find the node in the [map] at the given [path].
 * @param path The path to the node to find, as a list of either `String` (name of field in object) or `Int` (index of element in array).
 */
internal fun nodeAtPath(map: JsonMap, path: List<Any>): Any? {
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

internal fun deepMergeObject(destination: MutableJsonMap, obj: JsonMap) {
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

internal fun BufferedSource.toJsonMap(): JsonMap = BufferedSourceJsonReader(this).readAny() as JsonMap

