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
internal fun kotlinNameForFragmentInterfaceFile(name: String) = capitalizedIdentifier(name)
internal fun kotlinNameForFragmentImplementation(name: String) = capitalizedIdentifier(name) + "Impl"
internal fun kotlinNameForInputObject(name: String) = capitalizedIdentifier(name)

internal fun kotlinNameForInputObjectAdapter(name: String) = capitalizedIdentifier(name) + "_InputAdapter"
internal fun kotlinNameForVariablesAdapter(name: String) = capitalizedIdentifier(name) + "_VariablesAdapter"
internal fun kotlinNameForFragmentVariablesAdapter(name: String) = capitalizedIdentifier(name) + "Impl_VariablesAdapter"
internal fun kotlinNameForResponseAdapter(name: String) = capitalizedIdentifier(name) + "_ResponseAdapter"
internal fun kotlinNameForResponseFields(name: String) =  capitalizedIdentifier(name) + "_ResponseFields"
internal fun kotlinNameForFragmentResponseAdapter(name: String) = capitalizedIdentifier(name) + "Impl_ResponseAdapter"

// variables keep the same case as their declared name
internal fun kotlinNameForVariable(name: String) = regularIdentifier(name)
internal fun kotlinNameForProperty(name: String) = regularIdentifier(name)

private fun regularIdentifier(name: String) = name.escapeKotlinReservedWord()
private fun upperCaseIdentifier(name: String) = name.toUpperCase().escapeKotlinReservedWord()
private fun capitalizedIdentifier(name: String): String {
  return capitalizeFirstLetter(name).escapeKotlinReservedWord()
}

/**
 * A variation of [String.capitalize] that skips initial underscore, especially found in introspection queries
 *
 * There can still be name clashes if a property starts with an upper case letter
 */
internal fun capitalizeFirstLetter(name: String): String {
  val builder = StringBuilder(name.length)
  var isCapitalized = false
  name.forEach {
    builder.append(if (!isCapitalized && it.isLetter()) {
      isCapitalized = true
      it.toUpperCase()
    } else {
      it
    })
  }
  return builder.toString()
}

internal fun decapitalizeFirstLetter(name: String): String {
  val builder = StringBuilder(name.length)
  var isDecapitalized = false
  name.forEach {
    builder.append(if (!isDecapitalized && it.isLetter()) {
      isDecapitalized = true
      it.toLowerCase()
    } else {
      it
    })
  }
  return builder.toString()
}

internal fun isFirstLetterUpperCase(name: String): Boolean {
  return name.firstOrNull { it.isLetter() }?.isUpperCase() ?: true
}

internal fun CodeGenerationAst.TypeRef.fragmentPropertyName(): String {
  return if (this.isNamedFragmentDataRef) {
    this.enclosingType!!.name.decapitalize().escapeKotlinReservedWord()
  } else {
    "as${this.name.capitalize().escapeKotlinReservedWord()}"
  }
}

fun adapterPackageName(packageName: String) = "$packageName.adapter"
fun responseFieldsPackageName(packageName: String) = "$packageName.responsefields"
