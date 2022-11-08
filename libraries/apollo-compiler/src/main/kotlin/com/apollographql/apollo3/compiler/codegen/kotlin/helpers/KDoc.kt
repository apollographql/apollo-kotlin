package com.apollographql.apollo3.compiler.codegen.kotlin.helpers

import com.apollographql.apollo3.compiler.applyIf
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinResolver
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinSymbols
import com.apollographql.apollo3.compiler.ir.IrEnum
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
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

internal fun TypeSpec.Builder.maybeAddRequiresOptIn(resolver: KotlinResolver, optInFeature: String?): TypeSpec.Builder {
  if (optInFeature.isNullOrBlank()) {
    return this
  }

  val annotation = resolver.resolveRequiresOptInAnnotation() ?: return this
  return addAnnotation(AnnotationSpec.builder(annotation).build())
}

internal fun PropertySpec.Builder.maybeAddRequiresOptIn(resolver: KotlinResolver, optInFeature: String?): PropertySpec.Builder {
  if (optInFeature.isNullOrBlank()) {
    return this
  }

  val annotation = resolver.resolveRequiresOptInAnnotation() ?: return this
  return addAnnotation(AnnotationSpec.builder(annotation).build())
}

internal fun ParameterSpec.Builder.maybeAddRequiresOptIn(resolver: KotlinResolver, optInFeature: String?): ParameterSpec.Builder {
  if (optInFeature.isNullOrBlank()) {
    return this
  }

  val annotation = resolver.resolveRequiresOptInAnnotation() ?: return this
  return addAnnotation(AnnotationSpec.builder(annotation).build())
}

internal val suppressDeprecationAnnotationSpec = AnnotationSpec.builder(KotlinSymbols.Suppress)
    .addMember("%S", "DEPRECATION")
    .build()

internal fun FunSpec.Builder.maybeSuppressDeprecation(enumValues: List<IrEnum.Value>): FunSpec.Builder = applyIf(enumValues.any { !it.deprecationReason.isNullOrBlank() }) {
  addAnnotation(suppressDeprecationAnnotationSpec)
}

internal fun requiresOptInAnnotation(annotation: ClassName): AnnotationSpec {
  return AnnotationSpec.builder(KotlinSymbols.OptIn)
      .addMember(CodeBlock.of("%T::class", annotation))
      .build()
}

internal fun FunSpec.Builder.maybeAddOptIn(
    resolver: KotlinResolver,
    enumValues: List<IrEnum.Value>,
): FunSpec.Builder = applyIf(enumValues.any { !it.optInFeature.isNullOrBlank() }) {
  val annotation = resolver.resolveRequiresOptInAnnotation() ?: return@applyIf
  addAnnotation(requiresOptInAnnotation(annotation))
}
