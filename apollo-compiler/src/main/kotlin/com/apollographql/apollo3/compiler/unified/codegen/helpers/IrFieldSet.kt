package com.apollographql.apollo3.compiler.unified.codegen.helpers

import com.apollographql.apollo3.compiler.applyIf
import com.apollographql.apollo3.compiler.backend.codegen.kotlinNameForModel
import com.apollographql.apollo3.compiler.backend.codegen.makeDataClass
import com.apollographql.apollo3.compiler.backend.codegen.makeDataClassFromProperties
import com.apollographql.apollo3.compiler.unified.IrFieldSet
import com.apollographql.apollo3.compiler.unified.codegen.typeName
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

fun IrFieldSet.typeSpec(): TypeSpec {
  val properties = fields.map {
    PropertySpec.builder(
        it.responseName,
        it.typeName()
    ).applyIf(it.override) {
      addModifiers(KModifier.OVERRIDE)
    }.build()
  }

  val nestedTypes = fields.flatMap {
    it.fieldSets.map { it.typeSpec() }
  }

  val superInterfaces = implements.map { it.typeName() }

  val kotlinName = kotlinNameForModel(typeSet - fieldType, responseName)

  return if (possibleTypes.isEmpty()) {
    TypeSpec.interfaceBuilder(kotlinName)
        .addProperties(properties)
        .addTypes(nestedTypes)
        .addSuperinterfaces(superInterfaces)
        .build()
  } else {
    TypeSpec.classBuilder(kotlinName)
        .makeDataClassFromProperties(properties)
        .addTypes(nestedTypes)
        .addSuperinterfaces(superInterfaces)
        .build()
  }
}