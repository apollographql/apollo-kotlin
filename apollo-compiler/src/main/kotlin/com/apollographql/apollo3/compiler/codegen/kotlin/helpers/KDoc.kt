package com.apollographql.apollo3.compiler.codegen.kotlin.helpers

import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinResolver
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ParameterSpec
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

internal fun ParameterSpec.Builder.maybeAddDescription(description: String?): ParameterSpec.Builder {
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

internal fun ParameterSpec.Builder.maybeAddDeprecation(deprecationReason: String?): ParameterSpec.Builder {
  if (deprecationReason.isNullOrBlank()) {
    return this
  }

  return addAnnotation(deprecatedAnnotation(deprecationReason))
}

internal fun TypeSpec.Builder.maybeAddExperimental(resolver: KotlinResolver, experimentalReason: String?): TypeSpec.Builder {
  if (experimentalReason.isNullOrBlank()) {
    return this
  }

  val annotation = resolver.resolveExperimentalAnnotation() ?: return this
  return addAnnotation(AnnotationSpec.builder(annotation).build())
}

internal fun PropertySpec.Builder.maybeAddExperimental(resolver: KotlinResolver, experimentalReason: String?): PropertySpec.Builder {
  if (experimentalReason.isNullOrBlank()) {
    return this
  }

  val annotation = resolver.resolveExperimentalAnnotation() ?: return this
  return addAnnotation(AnnotationSpec.builder(annotation).build())
}

internal fun ParameterSpec.Builder.maybeAddExperimental(resolver: KotlinResolver, experimentalReason: String?): ParameterSpec.Builder {
  if (experimentalReason.isNullOrBlank()) {
    return this
  }

  val annotation = resolver.resolveExperimentalAnnotation() ?: return this
  return addAnnotation(AnnotationSpec.builder(annotation).build())
}
