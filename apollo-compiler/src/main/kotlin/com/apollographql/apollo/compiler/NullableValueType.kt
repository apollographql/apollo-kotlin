package com.apollographql.apollo.compiler

enum class NullableValueType(val value: String) {
  ANNOTATED("annotated"),
  APOLLO_OPTIONAL("apolloOptional"),
  GUAVA_OPTIONAL("guavaOptional"),
  JAVA_OPTIONAL("javaOptional"),
  INPUT_TYPE("inputType");
}