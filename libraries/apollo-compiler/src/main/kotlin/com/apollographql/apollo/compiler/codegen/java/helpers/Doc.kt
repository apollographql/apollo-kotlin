package com.apollographql.apollo.compiler.codegen.java.helpers


import com.apollographql.apollo.compiler.codegen.java.L
import com.apollographql.apollo.compiler.internal.applyIf
import com.apollographql.apollo.compiler.ir.IrEnum
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeSpec

/**
 * It's not 100% clear what the exact syntax of Javadoc is, but some HTML tags are allowed.
 *
 * GraphQL is markdown so that doesn't map to the Javadoc. For now, we're just preventing
 * parsing errors from markdown that contains nested comments.
 *
 * TODO: see if we can format that markdown better for Javadocs
 */
private fun String.escape(): String {
  return this.replace("*/", "*&#47;")
}

internal fun TypeSpec.Builder.maybeAddDescription(description: String?): TypeSpec.Builder {
  if (description.isNullOrBlank()) {
    return this
  }

  return addJavadoc("$L\n", description.escape())
}

internal fun FieldSpec.Builder.maybeAddDescription(description: String?): FieldSpec.Builder {
  if (description.isNullOrBlank()) {
    return this
  }

  return addJavadoc("$L\n", description.escape())
}

internal fun TypeSpec.Builder.maybeAddDeprecation(deprecationReason: String?): TypeSpec.Builder {
  if (deprecationReason == null) {
    return this
  }

  return addJavadoc("$L\n", deprecationReason.escape()).addAnnotation(deprecatedAnnotation())
}

internal fun FieldSpec.Builder.maybeAddDeprecation(deprecationReason: String?): FieldSpec.Builder {
  if (deprecationReason == null) {
    return this
  }

  return addJavadoc("$L\n", deprecationReason.escape()).addAnnotation(deprecatedAnnotation())
}

internal fun MethodSpec.Builder.maybeSuppressDeprecation(enumValues: List<IrEnum.Value>): MethodSpec.Builder = applyIf(enumValues.any { it.deprecationReason != null }) {
  addAnnotation(suppressDeprecatedAnnotation())
}
