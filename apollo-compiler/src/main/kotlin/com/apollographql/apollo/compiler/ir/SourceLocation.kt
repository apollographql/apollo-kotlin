package com.apollographql.apollo.compiler.ir

data class SourceLocation(val line: Int, val position: Int) {
  companion object {
    val UNKNOWN = SourceLocation(-1, -1)
  }
}
