package com.apollographql.apollo3.compiler.codegen

internal interface Layout {
  fun schemaPackageName(): String
  fun executableDocumentPackageName(filePath: String?): String

  fun operationName(name: String, capitalizedOperationType: String): String
  fun fragmentName(name: String): String
  fun schemaTypeName(schemaTypeName: String): String
  fun topLevelName(name: String): String

  fun propertyName(name: String): String
}