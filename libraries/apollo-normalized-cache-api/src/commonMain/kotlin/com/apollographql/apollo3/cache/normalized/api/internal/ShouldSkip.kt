package com.apollographql.apollo3.cache.normalized.api.internal

import com.apollographql.apollo3.api.CompiledField
import com.apollographql.apollo3.ast.GQLBooleanValue
import com.apollographql.apollo3.ast.GQLField
import com.apollographql.apollo3.ast.GQLValue
import com.apollographql.apollo3.ast.GQLVariableDefinition
import com.apollographql.apollo3.ast.GQLVariableValue

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

internal fun GQLField.shouldSkip(variableValues: Map<String, Any?>): Boolean {
  directives.forEach {
    if (name == "skip") {
      if (it.arguments.isEmpty()) {
        return true
      }
      val argumentValue = it.arguments.first().value
      if (argumentValue is GQLBooleanValue) {
        return argumentValue.value
      } else if (argumentValue is GQLVariableValue) {
        return (variableValues[argumentValue.name] as? Boolean) ?: false
      }
    } else if (name == "include") {
      if (it.arguments.isEmpty()) {
        return false
      }
      val argumentValue = it.arguments.first().value
      if (argumentValue is GQLBooleanValue) {
        return !argumentValue.value
      } else if (argumentValue is GQLVariableValue) {
        return !((variableValues[argumentValue.name] as? Boolean) ?: false)
      }
    }
  }

  return false
}

