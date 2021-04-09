package com.apollographql.apollo3.compiler.unified.codegen.helpers

import com.apollographql.apollo3.compiler.applyIf
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
    fieldSets.map {
      val accessors = if (it == typeFieldSet) accessors(true) else emptyList()
      it.typeSpec(true, accessors)
    }
  } else {
    val interfacesTypeSpecs = interfaces.map {
      val accessors = if (it == typeFieldSet) accessors(false) else emptyList()
      it.typeSpec(true, accessors)
    }
    val implementationTypeSpecs = implementations.map { it.typeSpec(false, emptyList()) }

    interfacesTypeSpecs + implementationTypeSpecs
  }
}

class Accessor(val name: String, val typeName: TypeName)

private fun IrField.accessors(asInterface: Boolean): List<Accessor> {
  val inlineAccessors = fieldSets.filter { it != typeFieldSet }
      .map { it.typeSet }
      .map { typeSet ->
        val target = if (asInterface) {
          fieldSets.first { it.typeSet == typeSet }
        } else {
          implementations.firstOrNull { it.typeSet == typeSet }
              ?: interfaces.first { it.typeSet == typeSet }
        }
        Accessor(
            name = "as${target.fullPath.elements.last()}",
            typeName = target.fullPath.typeName()
        )
      }

  val fragmentAccessors = fragmentAccessors.map {
    Accessor(
        name = it.name.decapitalize(),
        typeName = it.path.typeName()
    )
  }

  return inlineAccessors + fragmentAccessors
}


private fun companionTypeSpec(receiverTypeName: TypeName, accessors: List<Accessor>): TypeSpec {
  val funSpecs = accessors.map { accessor ->
    FunSpec.builder(accessor.name)
        .receiver(receiverTypeName)
        .addCode("return this as? %T\n", accessor.typeName)
        .build()
  }
  return TypeSpec.companionObjectBuilder()
      .addFunctions(funSpecs)
      .build()
}

fun IrFieldSet.typeSpec(asInterface: Boolean, accessors: List<Accessor>): TypeSpec {
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
        .applyIf(accessors.isNotEmpty()) {
          addType(companionTypeSpec(fullPath.typeName(), accessors))
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