package com.apollographql.apollo3.api.internal

import com.apollographql.apollo3.api.ResponseField

fun ResponseField.shouldSkip(variableValues: Map<String, Any?>): Boolean {
  for (condition in conditions) {
    if (condition is ResponseField.BooleanCondition) {
      val conditionValue = variableValues[condition.variableName] as Boolean
      if (condition.isInverted) {
        // means it's a skip directive
        if (conditionValue) {
          return true
        }
      } else {
        // means it's an include directive
        if (!conditionValue) {
          return true
        }
      }
    }
  }
  return false
}