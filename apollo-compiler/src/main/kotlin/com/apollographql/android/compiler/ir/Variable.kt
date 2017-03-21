package com.apollographql.android.compiler.ir

data class Variable(
    val name: String,
    val type: String
) {
  fun optional(): Boolean = !type.endsWith(suffix = "!")
}