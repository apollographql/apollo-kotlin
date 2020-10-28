package com.apollographql.apollo.compiler.ir

data class Variable(
    val name: String,
    val type: String,
    val defaultValue: Any?,
    val sourceLocation: SourceLocation
) {
  fun optional(): Boolean = !type.endsWith(suffix = "!")
}
