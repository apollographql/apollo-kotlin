package com.apollographql.apollo.compiler.frontend.ir

sealed class ScalarType(val name: String) {
  object ID : ScalarType("ID")
  object STRING : ScalarType("String")
  object INT : ScalarType("Int")
  object BOOLEAN : ScalarType("Boolean")
  object FLOAT : ScalarType("Float")

  companion object {
    fun forName(name: String): ScalarType? = when (name) {
      STRING.name -> STRING
      INT.name -> INT
      BOOLEAN.name -> BOOLEAN
      FLOAT.name -> FLOAT
      else -> null
    }
  }
}
