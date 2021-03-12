package com.apollographql.apollo3.compiler.backend.codegen

import com.apollographql.apollo3.api.CustomScalar
import com.apollographql.apollo3.compiler.applyIf
import com.apollographql.apollo3.compiler.backend.ast.CodeGenerationAst
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName

internal fun Collection<CodeGenerationAst.CustomScalarType>.typeSpec(generateAsInternal: Boolean): TypeSpec {
  return TypeSpec.objectBuilder("CustomScalars")
      .applyIf(generateAsInternal) { addModifiers(KModifier.INTERNAL) }
      .addKdoc("Auto generated constants for custom scalars. Use them to register your [ResponseAdapter]s")
      .apply {
        forEach {
          addProperty(it.propertySpec())
        }
      }
      .build()
}

private fun CodeGenerationAst.CustomScalarType.propertySpec(): PropertySpec {
  return PropertySpec
      .builder(name.capitalize(), CustomScalar::class)
      .initializer("%T(%S, %S)", CustomScalar::class.asTypeName(), schemaType, mappedType)
      .build()
}
