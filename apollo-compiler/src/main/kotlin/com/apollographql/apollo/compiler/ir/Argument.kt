package com.apollographql.apollo.compiler.ir

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Argument(
    val name: String,
    /**
     * The value of the argument
     * - Input objects will be represented as Maps
     * - Built-in scalars as their primitive type
     * - Variables as a map: {"kind": "Variable", "variableName": name}
     */
    val value: Any?,
    /**
     * The type of the argument as it would appear in a query
     * For an example: String, [String!]!, SomeInputObject, SomeEnum
     */
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
