package com.apollographql.apollo3.compiler.unified.codegen.helpers

import com.apollographql.apollo3.compiler.applyIf
import com.apollographql.apollo3.compiler.backend.codegen.capitalizeFirstLetter
import com.apollographql.apollo3.compiler.backend.codegen.kotlinNameForProperty
import com.apollographql.apollo3.compiler.backend.codegen.makeDataClassFromProperties
import com.apollographql.apollo3.compiler.unified.IrField
import com.apollographql.apollo3.compiler.unified.IrFieldSet
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

fun IrField.typeSpecs(asInterface: Boolean): List<TypeSpec> {

  return if (asInterface) {
    fieldSets.map { it.typeSpec(true, this) }
  } else {
    val interfacesTypeSpecs = interfaces.map { it.typeSpec(true, this) }
    val implementationTypeSpecs = implementations.map { it.typeSpec(false, this) }

    interfacesTypeSpecs + implementationTypeSpecs
  }
}

private fun TypeSpec.Builder.addAccessors(field: IrField) = apply {
  field.inlineAccessors.forEach { inlineAccessor ->
    val name = (inlineAccessor.typeSet /*- field.typeFieldSet!!.typeSet*/).sorted().map { capitalizeFirstLetter(it) }.joinToString("")

    //field.fieldSets.first { it.typeSet == inlineAccessor.typeSet }.modelName

    addFunction(
        FunSpec.builder("as$name")
            .addCode("return this as? %T\n", inlineAccessor.path.typeName())
            .applyIf(inlineAccessor.override) {
              addModifiers(KModifier.OVERRIDE)
            }
            .build()
    )
  }
  field.fragmentAccessors.forEach {
    val name = it.name.decapitalize()
    addFunction(
        FunSpec.builder(name)
            .addCode("return this as? %T\n", it.path.typeName())
            .applyIf(it.override) {
              addModifiers(KModifier.OVERRIDE)
            }
            .build()
    )
  }
}

fun IrFieldSet.typeSpec(asInterface: Boolean, field: IrField): TypeSpec {
  val properties = fields.map {
    PropertySpec.builder(kotlinNameForProperty(it.responseName), it.typeName())
        .applyIf(it.override) { addModifiers(KModifier.OVERRIDE) }
        .maybeAddDescription(it.description)
        .maybeAddDeprecation(it.deprecationReason)
        .build()
  }

  val nestedTypes = fields.flatMap {
    it.typeSpecs(asInterface)
  }

  val superInterfaces = implements.map { it.typeName() }

  return if (asInterface) {
    TypeSpec.interfaceBuilder(modelName)
        .addProperties(properties)
        .addTypes(nestedTypes)
        .applyIf(this == field.typeFieldSet) {
          addAccessors(field)
        }
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