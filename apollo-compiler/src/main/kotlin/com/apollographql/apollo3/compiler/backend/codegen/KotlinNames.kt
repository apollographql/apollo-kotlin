package com.apollographql.apollo3.compiler.backend.codegen

import com.apollographql.apollo3.compiler.backend.ast.CodeGenerationAst
import com.apollographql.apollo3.compiler.escapeKotlinReservedWord

/**
 * This file contains GraphQL -> Kotlin transformations
 */

internal fun kotlinNameForCustomScalar(name: String) = capitalizedIdentifier(name)

internal fun kotlinNameForEnum(name: String) = regularIdentifier(name)
internal fun kotlinNameForEnumValue(name: String) = upperCaseIdentifier(name)

internal fun kotlinNameForOperation(name: String) = capitalizedIdentifier(name)
internal fun kotlinNameForFragment(name: String) = capitalizedIdentifier(name)
internal fun kotlinNameForInputObject(name: String) = capitalizedIdentifier(name)
internal fun kotlinNameForInputObjectAdapter(name: String) = capitalizedIdentifier(name) + "_InputAdapter"
internal fun kotlinNameForVariablesAdapter(name: String) = capitalizedIdentifier(name) + "_VariablesAdapter"
internal fun kotlinNameForResponseAdapter(name: String) = capitalizedIdentifier(name) + "_ResponseAdapter"
internal fun kotlinNameForResponseFields(name: String) =  capitalizedIdentifier(name) + "_ResponseFields"

// variables keep the same case as their declared name
internal fun kotlinNameForVariable(name: String) = regularIdentifier(name)
internal fun kotlinNameForProperty(name: String) = regularIdentifier(name)

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
fun responseFieldsPackageName(packageName: String) = "$packageName.responsefields"
