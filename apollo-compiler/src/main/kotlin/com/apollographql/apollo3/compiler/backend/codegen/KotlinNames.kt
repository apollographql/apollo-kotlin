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
internal fun kotlinNameForSerializer(operationName: String) = kotlinNameForOperation(operationName) + "_Adapter"
internal fun kotlinNameForVariable(variableName: String) = decapitalizedIdentifier(variableName)

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