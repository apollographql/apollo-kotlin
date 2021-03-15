package com.apollographql.apollo3.compiler.backend.codegen

import com.apollographql.apollo3.api.CustomScalar
import com.apollographql.apollo3.compiler.backend.ast.CodeGenerationAst
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName

internal fun Collection<CodeGenerationAst.CustomScalarType>.typeSpec(): TypeSpec {
  return TypeSpec.objectBuilder("CustomScalars")
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
