package com.apollographql.apollo3.compiler.codegen.kotlin.helpers

import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

internal fun TypeSpec.Builder.maybeAddDescription(description: String?): TypeSpec.Builder {
  if (description.isNullOrBlank()) {
    return this
  }

  return addKdoc("%L", description)
}

internal fun PropertySpec.Builder.maybeAddDescription(description: String?): PropertySpec.Builder {
  if (description.isNullOrBlank()) {
    return this
  }

  return addKdoc("%L", description)
}

internal fun TypeSpec.Builder.maybeAddDeprecation(deprecationReason: String?): TypeSpec.Builder {
  if (deprecationReason.isNullOrBlank()) {
    return this
  }

  return addAnnotation(deprecatedAnnotation(deprecationReason))
}

internal fun PropertySpec.Builder.maybeAddDeprecation(deprecationReason: String?): PropertySpec.Builder {
  if (deprecationReason.isNullOrBlank()) {
    return this
  }

  return addAnnotation(deprecatedAnnotation(deprecationReason))
}
