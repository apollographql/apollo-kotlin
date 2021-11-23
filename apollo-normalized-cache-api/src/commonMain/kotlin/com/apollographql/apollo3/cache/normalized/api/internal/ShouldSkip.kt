package com.apollographql.apollo3.cache.normalized.api.internal

import com.apollographql.apollo3.api.CompiledField

internal fun CompiledField.shouldSkip(variableValues: Map<String, Any?>): Boolean {
  condition.forEach {
    var value = (variableValues.get(it.name) as? Boolean) ?: false
    if (it.inverted) {
      value = !value
    }
    if (!value) {
      return true
    }
  }

  return false
}
