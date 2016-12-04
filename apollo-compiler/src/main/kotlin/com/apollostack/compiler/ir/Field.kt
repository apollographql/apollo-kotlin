package com.apollostack.compiler.ir

class Field(val responseName: String, val fieldName: String, val type: String,
    val fields: List<Field>?) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other?.javaClass != javaClass) return false

    other as Field

    if (type != other.type) return false

    return true
  }

  override fun hashCode(): Int {
    return type.hashCode()
  }

}
