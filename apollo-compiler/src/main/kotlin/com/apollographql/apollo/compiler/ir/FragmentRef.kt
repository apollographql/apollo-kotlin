package com.apollographql.apollo.compiler.ir

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FragmentRef(
    val name: String,
    val conditions: List<Condition>,
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
