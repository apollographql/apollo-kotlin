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
import com.squareup.kotlinpoet.TypeName
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

private fun companionTypeSpec(receiverTypeName: TypeName, field: IrField): TypeSpec {
  val inlineAccessors = field.inlineAccessors.map { inlineAccessor ->
    val name = inlineAccessor.path.elements.last()
    FunSpec.builder("as$name")
        .receiver(receiverTypeName)
        .addCode("return this as? %T\n", inlineAccessor.path.typeName())
        .build()
  }
  val fragmentAccessors = field.fragmentAccessors.map { fragmentAccessors ->
    val name = fragmentAccessors.path.elements.last()

    FunSpec.builder("as$name")
        .receiver(receiverTypeName)
        .addCode("return this as? %T\n", fragmentAccessors.path.typeName())
        .build()
  }
  return TypeSpec.companionObjectBuilder()
      .addFunctions(inlineAccessors)
      .addFunctions(fragmentAccessors)
      .build()
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
        .applyIf(this == field.typeFieldSet
            && (field.fragmentAccessors.isNotEmpty() || field.inlineAccessors.isNotEmpty())) {
          addType(companionTypeSpec(fullPath.typeName(), field))
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