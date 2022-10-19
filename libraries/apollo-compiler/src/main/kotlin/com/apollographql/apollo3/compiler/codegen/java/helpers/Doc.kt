package com.apollographql.apollo3.compiler.codegen.java.helpers


import com.apollographql.apollo3.compiler.applyIf
import com.apollographql.apollo3.compiler.codegen.java.JavaClassNames
import com.apollographql.apollo3.compiler.codegen.java.L
import com.apollographql.apollo3.compiler.codegen.java.S
import com.apollographql.apollo3.compiler.ir.IrEnum
import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec
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

internal fun MethodSpec.Builder.maybeSuppressDeprecation(enumValues: List<IrEnum.Value>): MethodSpec.Builder = applyIf(enumValues.any { !it.deprecationReason.isNullOrBlank() }) {
  addAnnotation(AnnotationSpec.builder(JavaClassNames.SuppressWarnings)
      .addMember("value", S, "deprecation")
      .build()
  )
}
