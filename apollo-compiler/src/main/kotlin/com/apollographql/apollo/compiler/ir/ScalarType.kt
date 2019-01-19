package com.apollographql.apollo.compiler.ir

sealed class ScalarType(val name: String) {
  object ID : ScalarType("ID")
  object STRING : ScalarType("String")
  object INT : ScalarType("Int")
  object BOOLEAN : ScalarType("Boolean")
  object FLOAT : ScalarType("Float")

  companion object {
    fun forName(name: String): ScalarType? = when (name) {
      ScalarType.STRING.name -> ScalarType.STRING
      ScalarType.INT.name -> ScalarType.INT
      ScalarType.BOOLEAN.name -> ScalarType.BOOLEAN
      ScalarType.FLOAT.name -> ScalarType.FLOAT
      else -> null
    }
  }
}