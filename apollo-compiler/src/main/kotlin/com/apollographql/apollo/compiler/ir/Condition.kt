package com.apollographql.apollo.compiler.ir

data class Condition(
    val kind: String,
    val variableName: String,
    val inverted: Boolean
) {

  enum class Kind(val rawValue: String) {
    BOOLEAN("BooleanCondition")
  }
}