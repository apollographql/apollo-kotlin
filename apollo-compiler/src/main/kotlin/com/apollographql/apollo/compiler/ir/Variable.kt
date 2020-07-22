package com.apollographql.apollo.compiler.ir

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Variable(
    val name: String,
    val type: String,
    val sourceLocation: SourceLocation
) {
  fun optional(): Boolean = !type.endsWith(suffix = "!")
}
