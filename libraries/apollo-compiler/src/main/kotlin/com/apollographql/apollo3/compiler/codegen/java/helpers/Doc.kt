package com.apollographql.apollo3.compiler.codegen.java.helpers


import com.apollographql.apollo3.compiler.codegen.java.L
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.TypeSpec

internal fun TypeSpec.Builder.maybeAddDescription(description: String?): TypeSpec.Builder {
  if (description.isNullOrBlank()) {
    return this
  }

  return addJavadoc("$L\n", description)
}

internal fun FieldSpec.Builder.maybeAddDescription(description: String?): FieldSpec.Builder {
  if (description.isNullOrBlank()) {
    return this
  }

  return addJavadoc("$L\n", description)
}

internal fun TypeSpec.Builder.maybeAddDeprecation(deprecationReason: String?): TypeSpec.Builder {
  if (deprecationReason.isNullOrBlank()) {
    return this
  }

  return addJavadoc("$L\n", deprecationReason).addAnnotation(deprecatedAnnotation())
}

internal fun FieldSpec.Builder.maybeAddDeprecation(deprecationReason: String?): FieldSpec.Builder {
  if (deprecationReason.isNullOrBlank()) {
    return this
  }

  return addJavadoc("$L\n", deprecationReason).addAnnotation(deprecatedAnnotation())
}
