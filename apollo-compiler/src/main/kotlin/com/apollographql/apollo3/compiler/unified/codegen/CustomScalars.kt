package com.apollographql.apollo3.compiler.unified.codegen

import com.apollographql.apollo3.api.CustomScalar
import com.apollographql.apollo3.compiler.backend.ast.CodeGenerationAst
import com.apollographql.apollo3.compiler.backend.codegen.kotlinNameForEnum
import com.apollographql.apollo3.compiler.unified.IrCustomScalar
import com.apollographql.apollo3.compiler.unified.TypeSet
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName

internal fun List<IrCustomScalar>.typeSpec(userMapping: Map<String, String>): TypeSpec {
  return TypeSpec.objectBuilder("CustomScalars")
      .addKdoc("Auto generated constants for custom scalars. Use them to register your [ResponseAdapter]s")
      .addProperties(
          map {
            val kotlinType = userMapping.getOrDefault(it.name, "kotlin.Any")
            PropertySpec
                .builder(kotlinNameForEnum(it.name), CustomScalar::class)
                .initializer("%T(%S, %S)", CustomScalar::class.asTypeName(), it.name, kotlinType)
                .build()
          }
      )
      .build()
}
