package com.apollographql.apollo.compiler.ir

data class Variable(
    val name: String,
    val type: String,
    val sourceLocation: SourceLocation = SourceLocation.UNKNOWN
) {
  fun optional(): Boolean = !type.endsWith(suffix = "!")
}
