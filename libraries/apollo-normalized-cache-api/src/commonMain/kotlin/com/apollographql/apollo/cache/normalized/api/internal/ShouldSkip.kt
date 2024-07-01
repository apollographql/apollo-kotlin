package com.apollographql.apollo.cache.normalized.api.internal

import com.apollographql.apollo.api.CompiledCondition
import com.apollographql.apollo.api.CompiledField
import com.apollographql.apollo.api.CompiledFragment

internal fun CompiledField.shouldSkip(variableValues: Map<String, Any?>): Boolean {
  return condition.shouldSkip(variableValues)
}

private fun List<CompiledCondition>.shouldSkip(variableValues: Map<String, Any?>): Boolean {
  forEach {
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
internal fun CompiledFragment.shouldSkip(variableValues: Map<String, Any?>): Boolean {
  return condition.shouldSkip(variableValues)
}

