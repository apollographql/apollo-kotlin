package com.apollographql.apollo3.internal

internal fun Map<String, Any?>.deepCopy(): Map<String, Any?> {
  val copy = mutableMapOf<String, Any?>()
  for ((key, value) in this) {
    @Suppress("UNCHECKED_CAST")
    copy[key] = when (value) {
      is Map<*, *> -> (value as Map<String, Any?>).deepCopy()
      is List<*> -> (value as List<Any?>).deepCopy()
      else -> value
    }
  }
  return copy
}

private fun List<Any?>.deepCopy(): List<Any?> {
  val copy = mutableListOf<Any?>()
  for (item in this) {
    @Suppress("UNCHECKED_CAST")
    copy += when (item) {
      is Map<*, *> -> (item as Map<String, Any?>).deepCopy()
      is List<*> -> item.deepCopy()
      else -> item
    }
  }
  return copy
}
