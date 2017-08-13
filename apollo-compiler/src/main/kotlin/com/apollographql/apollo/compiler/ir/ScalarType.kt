package com.apollographql.apollo.compiler.ir

sealed class ScalarType(val name: String) {
  object ID : ScalarType("ID")
  object STRING : ScalarType("String")
  object INT : ScalarType("Int")
  object BOOLEAN : ScalarType("Boolean")
  object FLOAT : ScalarType("Float")
}