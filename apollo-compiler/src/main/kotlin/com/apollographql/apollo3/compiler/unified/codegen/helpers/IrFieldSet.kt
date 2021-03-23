package com.apollographql.apollo3.compiler.unified.codegen.helpers

import com.apollographql.apollo3.compiler.applyIf
import com.apollographql.apollo3.compiler.backend.codegen.kotlinNameForModel
import com.apollographql.apollo3.compiler.backend.codegen.makeDataClassFromProperties
import com.apollographql.apollo3.compiler.unified.IrField
import com.apollographql.apollo3.compiler.unified.IrFieldSet
import com.apollographql.apollo3.compiler.unified.codegen.typeName
import com.apollographql.apollo3.compiler.unified.combinations
import com.apollographql.apollo3.compiler.unified.intersection
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

fun IrField.typeSpecs(): List<TypeSpec> {
  val interfacesTypeSpecs = interfacesFieldSets.map { it.typeSpec(true) }
  val implementationTypeSpecs = implementationFieldSets.map { it.typeSpec(false) }

  return interfacesTypeSpecs + implementationTypeSpecs
}

fun IrFieldSet.typeSpec(asInterface: Boolean): TypeSpec {
  val properties = fields.map {
    PropertySpec.builder(
        it.responseName,
        it.typeName()
    ).applyIf(it.override) {
      addModifiers(KModifier.OVERRIDE)
    }.build()
  }

  val nestedTypes = fields.flatMap {
    it.typeSpecs()
  }

  val superInterfaces = implements.map { it.typeName() }

  return if (asInterface) {
    TypeSpec.interfaceBuilder(modelName)
        .addProperties(properties)
        .addTypes(nestedTypes)
        .addSuperinterfaces(superInterfaces)
        .build()
  } else {
    TypeSpec.classBuilder(modelName)
        .makeDataClassFromProperties(properties)
        .addTypes(nestedTypes)
        .addSuperinterfaces(superInterfaces)
        .build()
  }
}