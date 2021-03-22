package com.apollographql.apollo3.compiler.backend.codegen

import com.apollographql.apollo3.compiler.backend.ast.CodeGenerationAst
import com.apollographql.apollo3.compiler.escapeKotlinReservedWord

/**
 * This file contains GraphQL -> Kotlin transformations
 */

internal fun kotlinNameForCustomScalar(graphqlName: String) = upperCaseIdentifier(graphqlName)

internal fun kotlinNameForEnum(graphqlName: String) = regularIdentifier(graphqlName)
internal fun kotlinNameForEnumValue(graphqlName: String) = upperCaseIdentifier(graphqlName)

internal fun kotlinNameForOperation(operationName: String) = capitalizedIdentifier(operationName)
internal fun kotlinNameForInputObject(name: String) = capitalizedIdentifier(name)
internal fun kotlinNameForInputObjectAdapter(inputObjectName: String) = capitalizedIdentifier(inputObjectName) + "_InputAdapter"
internal fun kotlinNameForVariablesAdapter(operationName: String) = capitalizedIdentifier(operationName) + "_VariablesAdapter"

// variables keep the same case as their declared name
internal fun kotlinNameForVariable(graphqlName: String) = regularIdentifier(graphqlName)
internal fun kotlinNameForProperty(graphqlName: String) = regularIdentifier(graphqlName)

private fun regularIdentifier(name: String) = name.escapeKotlinReservedWord()
private fun upperCaseIdentifier(name: String) = name.toUpperCase().escapeKotlinReservedWord()
private fun capitalizedIdentifier(name: String) = name.capitalize().escapeKotlinReservedWord()

internal fun CodeGenerationAst.TypeRef.fragmentPropertyName(): String {
  return if (this.isNamedFragmentDataRef) {
    this.enclosingType!!.name.decapitalize().escapeKotlinReservedWord()
  } else {
    "as${this.name.capitalize().escapeKotlinReservedWord()}"
  }
}

fun adapterPackageName(packageName: String) = "$packageName.adapter"
fun adapterName(name: String) = "${name}_Adapter"
fun dataAdapterName(name: String) = "${name}_DataAdapter"