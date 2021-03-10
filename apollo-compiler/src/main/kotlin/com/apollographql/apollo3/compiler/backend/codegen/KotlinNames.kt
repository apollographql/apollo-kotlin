package com.apollographql.apollo3.compiler.backend.codegen

import com.apollographql.apollo3.compiler.backend.ast.CodeGenerationAst
import com.apollographql.apollo3.compiler.escapeKotlinReservedWord

/**
 * This file contains GraphQL -> Kotlin transformations
 *
 * this is mostly empty right now but it'd be nice to centralize everything here so we can have a central place to
 * control name generation
 */
internal fun kotlinNameForEnumValue(graphqlEnumValue: String) = graphqlEnumValue.toUpperCase()
internal fun kotlinNameForEnum(graphqlEnum: String) = graphqlEnum.escapeKotlinReservedWord()
internal fun kotlinNameForField(responseName: String) = responseName.escapeKotlinReservedWord()
internal fun kotlinNameForOperation(operationName: String) = operationName.escapeKotlinReservedWord()
internal fun kotlinNameForAdapterField(type: CodeGenerationAst.FieldType): String {
  return kotlinNameForAdapterFieldRecursive(type).decapitalize() + "Adapter"
}

internal fun kotlinNameForTypeCaseAdapterField(typeRef: CodeGenerationAst.TypeRef): String {
  return typeRef.name.escapeKotlinReservedWord() + "Adapter"
}

internal fun kotlinNameForInputObjectType(name: String) = capitalizedIdentifier(name)
internal fun kotlinNameForInputObjectAdapter(operationName: String) = kotlinNameForOperation(operationName) + "_InputAdapter"
internal fun kotlinNameForVariablesAdapter(operationName: String) = kotlinNameForOperation(operationName) + "_VariablesAdapter"
// variables keep the same case as their declared name
internal fun kotlinNameForVariable(variableName: String) = variableName.escapeKotlinReservedWord()

private fun decapitalizedIdentifier(name: String) = name.decapitalize().escapeKotlinReservedWord()
private fun capitalizedIdentifier(name: String) = name.capitalize().escapeKotlinReservedWord()

private fun kotlinNameForAdapterFieldRecursive(type: CodeGenerationAst.FieldType): String {
  if (type.nullable) {
    return "Nullable" + kotlinNameForAdapterFieldRecursive(type.nonNullable())
  }

  return when (type) {
    is CodeGenerationAst.FieldType.Array -> "ListOf" + kotlinNameForAdapterFieldRecursive(type.rawType)
    is CodeGenerationAst.FieldType.Object -> type.typeRef.name.capitalize()
    is CodeGenerationAst.FieldType.InputObject -> type.typeRef.name.capitalize()
    is CodeGenerationAst.FieldType.Scalar -> type.schemaTypeName.capitalize()
  }
}

internal fun CodeGenerationAst.TypeRef.fragmentVariableName(): String {
  return if (this.isNamedFragmentDataRef) {
    this.enclosingType!!.name.decapitalize().escapeKotlinReservedWord()
  } else {
    "as${this.name.capitalize().escapeKotlinReservedWord()}"
  }
}

internal fun CodeGenerationAst.TypeRef.fragmentResponseAdapterVariableName(): String {
  return if (this.isNamedFragmentDataRef) {
    "${this.enclosingType!!.name.escapeKotlinReservedWord()}Adapter"
  } else {
    "${this.name.escapeKotlinReservedWord()}Adapter"
  }
}
