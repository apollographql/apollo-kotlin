package com.apollographql.apollo.compiler.backend.codegen

import com.apollographql.apollo.api.ScalarType
import com.apollographql.apollo.compiler.applyIf
import com.apollographql.apollo.compiler.backend.ast.CodeGenerationAst
import com.apollographql.apollo.compiler.backend.ast.CustomScalarTypes
import com.apollographql.apollo.compiler.escapeKotlinReservedWord
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

internal fun CustomScalarTypes.typeSpec(generateAsInternal: Boolean = false): TypeSpec {
  return TypeSpec
      .enumBuilder("CustomScalar")
      .applyIf(generateAsInternal) { addModifiers(KModifier.INTERNAL) }
      .addSuperinterface(ScalarType::class.java)
      .apply {
        toSortedMap().map { (_, customScalarType) ->
          addEnumConstant(
              name = customScalarType.name.escapeKotlinReservedWord(),
              typeSpec = customScalarType.enumConstantTypeSpec()
          )
        }
      }
      .build()
}

private fun CodeGenerationAst.CustomScalarType.enumConstantTypeSpec(): TypeSpec {
  return TypeSpec
      .anonymousClassBuilder()
      .addProperty(PropertySpec.builder("graphqlName", String::class)
          .addModifiers(KModifier.OVERRIDE)
          .initializer("%S", schemaType)
          .build()
      )
      .addProperty(PropertySpec.builder("className", String::class)
          .addModifiers(KModifier.OVERRIDE)
          .initializer("%S", mappedType)
          .build()
      )
      .build()
}

