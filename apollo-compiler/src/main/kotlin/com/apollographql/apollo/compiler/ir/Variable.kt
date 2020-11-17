package com.apollographql.apollo.compiler.ir

data class Variable(
    val name: String,
    val type: String,
    val sourceLocation: SourceLocation,
    val defaultValue: Any?
) {
  fun optional(): Boolean = !type.endsWith(suffix = "!")
}
