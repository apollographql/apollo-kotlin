package com.apollographql.apollo3.cache.normalized.api.internal

import com.apollographql.apollo3.api.CompiledCondition
import com.apollographql.apollo3.api.CompiledField
import com.apollographql.apollo3.api.CompiledFragment

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

