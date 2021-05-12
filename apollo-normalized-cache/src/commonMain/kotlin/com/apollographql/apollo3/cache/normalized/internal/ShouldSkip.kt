package com.apollographql.apollo3.cache.normalized.internal

import com.apollographql.apollo3.api.MergedField
import com.apollographql.apollo3.api.evaluate

fun MergedField.shouldSkip(variableValues: Map<String, Any?>): Boolean {
  val variables = variableValues.filter {
    (it.value as? Boolean) == true
  }.map { it.key }
      .toSet()

  return !condition.evaluate { variables.contains(it.name) }
}