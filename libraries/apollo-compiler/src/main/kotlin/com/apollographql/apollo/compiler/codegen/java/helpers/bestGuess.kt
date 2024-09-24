package com.apollographql.apollo.compiler.codegen.java.helpers

import com.squareup.javapoet.ClassName
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName

/**
 * Best guess a type name. Handles simple generics like `Map<String, Integer>`, but no variance or wildcards.
 */
internal fun bestGuess(name: String): TypeName? {
  val className = ClassName.bestGuess(name.substringBefore('<').withPackage())
  val typeArgs = name.substringAfter('<', "").substringBefore('>', "")
      .split(',')
      .filterNot { it.isEmpty() }
      .map { it.trim() }
  return if (typeArgs.isEmpty()) {
    className
  } else {
    ParameterizedTypeName.get(className, *typeArgs.map { bestGuess(it) }.toTypedArray())
  }
}

private fun String.withPackage(): String {
  return if (this in commonJavaTypes) {
    "java.lang.${this}"
  } else if (this in commonJavaCollectionsTypes) {
    "java.util.${this}"
  } else {
    this
  }
}

private val commonJavaTypes = setOf(
    "Boolean",
    "Byte",
    "Character",
    "Double",
    "Float",
    "Integer",
    "Iterable",
    "Long",
    "Short",
    "String",
)

private val commonJavaCollectionsTypes = setOf(
    "Collection",
    "List",
    "Map",
    "Set",
)
