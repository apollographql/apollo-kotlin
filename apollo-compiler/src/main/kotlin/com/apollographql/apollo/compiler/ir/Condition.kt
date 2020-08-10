package com.apollographql.apollo.compiler.ir

import com.squareup.moshi.JsonClass

data class Condition(
    val kind: String,
    val variableName: String,
    val inverted: Boolean,
    val sourceLocation: SourceLocation
) {

  enum class Kind(val rawValue: String) {
    BOOLEAN("BooleanCondition")
  }
}
