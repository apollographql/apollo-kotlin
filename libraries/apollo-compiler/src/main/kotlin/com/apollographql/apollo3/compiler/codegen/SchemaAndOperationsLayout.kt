package com.apollographql.apollo3.compiler.codegen

internal interface CommonLayout {
  fun topLevelName(name: String): String
  fun propertyName(name: String): String
}

internal interface SchemaLayout : CommonLayout {
  fun schemaPackageName(): String
  fun schemaTypeName(schemaTypeName: String): String
}

internal interface OperationsLayout {
  fun executableDocumentPackageName(filePath: String?): String
  fun operationName(name: String, capitalizedOperationType: String): String
  fun fragmentName(name: String): String
}

internal interface SchemaAndOperationsLayout : SchemaLayout, OperationsLayout