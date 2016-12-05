package com.apollostack.compiler

import com.apollostack.compiler.ir.Field
import com.squareup.javapoet.TypeSpec
import java.util.*
import javax.lang.model.element.Modifier

class OperationTypeSpecBuilder(
  val operationName: String,
  val fields: List<Field>
) {

  fun build(): TypeSpec {
    val typeSpecs = buildTypeSpecs(operationName, fields, "")
    return typeSpecs
      .single { it.name == operationName }
      .toBuilder()
      .addTypes(typeSpecs.filter { it.name != operationName })
      .build()
  }

  private fun buildTypeSpecs(currentTypeName: String, fields: List<Field>, typeNamePrefix: String): List<TypeSpec> {
    val fieldToTypeMap = HashMap<Field, String>()
    val typeToFieldMap = HashMap<String, Field>()
    val nonScalarFields = fields.filter { it.fields?.any() ?: false }
    nonScalarFields.forEach {
      val fieldType = "$typeNamePrefix${it.type.normalizeTypeName()}"

      val existingMapping = typeToFieldMap[fieldType]
      if (existingMapping != null && it.fields != existingMapping.fields) {
        // If we already saw this type and it has a different set of fields, it means
        // we have the same type used multiple times with different fields. In this case,
        // we'll use Field.toTypeName() to disambiguate the name since the type cannot be
        // used more than once. We also need to use the explicit naming for the previously
        // seen type, so we need to also remove it and re-add with the explicit naming.

        val fieldNewType = "$typeNamePrefix${it.toTypeName()}"
        typeToFieldMap.put(fieldNewType, it)
        fieldToTypeMap.put(it, fieldNewType)

        val existingFieldType = "$typeNamePrefix${it.type.normalizeTypeName()}"
        val existingFieldNewType = "$typeNamePrefix${existingMapping.toTypeName()}"
        typeToFieldMap.put(existingFieldNewType, existingMapping)
        typeToFieldMap.remove(existingFieldType)
        fieldToTypeMap.put(existingMapping, existingFieldNewType)
      } else {
        typeToFieldMap.put(fieldType, it)
        fieldToTypeMap.put(it, fieldType)
      }
    }

    val resultTypeSpecs = ArrayList<TypeSpec>()
    resultTypeSpecs.add(
      TypeSpec.interfaceBuilder(currentTypeName)
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
        .addMethods(fields.map { it.toMethodSpec(fieldToTypeMap[it]) })
        .build()
    )

    return typeToFieldMap.toList().fold(resultTypeSpecs) { typeSpecs, it ->
      typeSpecs.addAll(buildTypeSpecs(it.first, it.second.fields!!, it.first))
      typeSpecs
    }
  }

  private fun String.normalizeTypeName() = removeSuffix("!").removePrefix("[").removeSuffix("]").removeSuffix("!")
}