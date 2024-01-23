package com.apollographql.apollo3.compiler.codegen

import com.apollographql.apollo3.compiler.PackageNameGenerator

internal interface Layout {
  val packageNameGenerator: PackageNameGenerator
  fun schemaTypeName(schemaTypeName: String): String
  fun basePackageName(): String
  fun operationName(name: String, capitalizedOperationType: String): String
  fun propertyName(name: String): String
}