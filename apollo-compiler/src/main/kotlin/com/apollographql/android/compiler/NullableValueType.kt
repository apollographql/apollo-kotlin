package com.apollographql.android.compiler

enum class NullableValueType(val value: String) {
  ANNOTATED("annotated"),
  APOLLO_OPTIONAL("apolloOptional"),
  GUAVA_OPTIONAL("guavaOptional");

  companion object {
    fun findByValue(value: String): NullableValueType? = NullableValueType.values().find { it.value == value }
  }
}