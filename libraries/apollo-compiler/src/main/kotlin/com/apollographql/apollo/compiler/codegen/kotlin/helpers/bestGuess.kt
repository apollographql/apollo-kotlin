package com.apollographql.apollo.compiler.codegen.kotlin.helpers

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName

/**
 * Best guess a type name. Handles simple generics like `Map<String, Int?>`, but no variance or wildcards.
 * Common types such as `String`, `List`, `Map` are automatically prefixed with `kotlin.` or `kotlin.collections.`.
 */
internal fun bestGuess(name: String): TypeName {
  val isNullable = name.endsWith('?')
  val className = ClassName.bestGuess(name.substringBeforeLast('?').substringBefore('<').withPackage())
  val typeArgs = name.substringAfter('<', "").substringBefore('>', "")
      .split(',')
      .filterNot { it.isEmpty() }
      .map { it.trim() }
  return if (typeArgs.isEmpty()) {
    className
  } else {
    className.parameterizedBy(typeArgs.map { bestGuess(it) })
  }
      .copy(nullable = isNullable)
}

private fun String.withPackage(): String {
  return if (this in commonKotlinTypes) {
    "kotlin.${this}"
  } else if (this in commonKotlinCollectionsTypes) {
    "kotlin.collections.${this}"
  } else {
    this
  }
}

private val commonKotlinTypes = setOf(
    "Any",
    "Boolean",
    "Byte",
    "Char",
    "CharSequence",
    "Double",
    "Float",
    "Int",
    "Long",
    "Number",
    "Short",
    "String",
    "UByte",
    "UInt",
    "ULong",
    "Unit",
    "UShort",
)

private val commonKotlinCollectionsTypes = setOf(
    "Array",
    "Collection",
    "Iterable",
    "List",
    "Map",
    "Set",
)
