package com.apollographql.apollo.compiler.ir

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Argument(
    val name: String,
    val value: Any?,
    val type: String,
    val sourceLocation: SourceLocation
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is Argument) return false

    if (name != other.name) return false
    if (value != other.value) return false
    if (type != other.type) return false

    return true
  }

  override fun hashCode(): Int {
    var result = name.hashCode()
    result = 31 * result + (value?.hashCode() ?: 0)
    result = 31 * result + type.hashCode()
    return result
  }
}
