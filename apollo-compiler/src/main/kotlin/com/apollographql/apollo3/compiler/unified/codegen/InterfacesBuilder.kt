package com.apollographql.apollo3.compiler.unified.codegen

import com.apollographql.apollo3.compiler.applyIf
import com.apollographql.apollo3.compiler.backend.codegen.kotlinNameForModel
import com.apollographql.apollo3.compiler.unified.IrField
import com.apollographql.apollo3.compiler.unified.IrFieldSet
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec


private fun IrField.typeName(): TypeName {
  return type.typeName()
}

fun IrFieldSet.toTypeSpec(): TypeSpec {
  return TypeSpec.interfaceBuilder(kotlinNameForModel(typeSet - fieldType, responseName))
      .addProperties(
          fields.map {
            PropertySpec.builder(
                it.responseName,
                it.typeName()
            ).applyIf(it.override) {
              addModifiers(KModifier.OVERRIDE)
            }.build()
          }
      )
      .addTypes(
          fields.flatMap {
            it.fieldSets.map { it.toTypeSpec() }
          }
      )
      .addSuperinterfaces(
          implements.map {
            it.typeName()
          }
      )
      .build()
}