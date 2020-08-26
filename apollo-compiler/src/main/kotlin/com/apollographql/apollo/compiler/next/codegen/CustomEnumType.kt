package com.apollographql.apollo.compiler.next.codegen

import com.apollographql.apollo.api.ScalarType
import com.apollographql.apollo.compiler.applyIf
import com.apollographql.apollo.compiler.next.ast.CodeGenerationAst
import com.apollographql.apollo.compiler.next.ast.CustomTypes
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec

internal fun CustomTypes.typeSpec(generateAsInternal: Boolean = false): TypeSpec {
  return TypeSpec
      .enumBuilder("CustomType")
      .applyIf(generateAsInternal) { addModifiers(KModifier.INTERNAL) }
      .addSuperinterface(ScalarType::class.java)
      .apply {
        toSortedMap().map { (_, customType) ->
          addEnumConstant(
              name = customType.name,
              typeSpec = customType.enumConstantTypeSpec()
          )
        }
      }
      .build()
}

private fun CodeGenerationAst.CustomType.enumConstantTypeSpec(): TypeSpec {
  return TypeSpec
      .anonymousClassBuilder()
      .addFunction(FunSpec.builder("typeName")
          .addModifiers(KModifier.OVERRIDE)
          .returns(String::class)
          .addStatement("return %S", schemaType)
          .build()
      )
      .addFunction(FunSpec.builder("className")
          .returns(String::class)
          .addModifiers(KModifier.OVERRIDE)
          .addStatement("return %S", mappedType)
          .build()
      )
      .build()
}

