package com.apollostack.compiler

import com.apollostack.compiler.ir.Field
import com.cesarferreira.pluralize.singularize
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier

class OperationTypeSpecBuilder(
    val operationName: String,
    val fields: List<Field>) {

  fun build(): TypeSpec {
    return buildTypeSpecs(operationName, fields)
  }

  private fun buildTypeSpecs(currentTypeName: String, fields: List<Field>): TypeSpec {
    val innerTypes = fields
        .filter(Field::isNonScalar)
        .map { buildTypeSpecs(it.responseName.capitalize().singularize(), it.fields!!) }
    return TypeSpec.interfaceBuilder(currentTypeName)
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
        .addMethods(fields.map(Field::toMethodSpec))
        .addTypes(innerTypes)
        .build()
  }
}