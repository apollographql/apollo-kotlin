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
  val classesFieldSets = fieldSets.filter { it.possibleTypes.isNotEmpty() }

  val classesTypeSpecs = classesFieldSets.map { it.typeSpec(false) }
  if (classesFieldSets.size == 1) {
    return classesTypeSpecs
  } else {
    val interfacesToGenerate = classesFieldSets.map { it.typeSet }.combinations()
        .filter {
          it.size >= 2
        }
        .map {
          it.intersection()
        }
        .toSet()

    val interfacesTypeSpecs = fieldSets.filter {
      interfacesToGenerate.contains(it.typeSet)
    }.map {
      it.typeSpec(true)
    }

    return interfacesTypeSpecs + classesTypeSpecs
  }
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

  val kotlinName = kotlinNameForModel(typeSet - fieldType, responseName)

  return if (asInterface) {
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