package com.apollographql.apollo.compiler.ir

data class FragmentRef(
    val name: String,
    val sourceLocation: SourceLocation
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is FragmentRef) return false

    if (name != other.name) return false

    return true
  }

  override fun hashCode(): Int {
    return name.hashCode()
  }
}
