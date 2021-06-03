package com.apollographql.apollo3.cache.normalized.internal

import com.apollographql.apollo3.api.CompiledField
import com.apollographql.apollo3.api.MergedField
import com.apollographql.apollo3.api.evaluate

fun MergedField.shouldSkip(variableValues: Map<String, Any?>): Boolean {
  val variables = variableValues.filter {
    (it.value as? Boolean) == true
  }.map { it.key }
      .toSet()

  return !condition.evaluate { variables.contains(it.name) }
}

fun CompiledField.shouldSkip(variableValues: Map<String, Any?>): Boolean {
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