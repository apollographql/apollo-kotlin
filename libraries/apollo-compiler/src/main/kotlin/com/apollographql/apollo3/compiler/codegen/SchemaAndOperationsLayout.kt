package com.apollographql.apollo3.compiler.codegen

interface CommonLayout {
  fun topLevelName(name: String): String
  fun propertyName(name: String): String
}

interface SchemaLayout : CommonLayout {
  fun schemaPackageName(): String
  fun schemaTypeName(schemaTypeName: String): String
}

interface OperationsLayout: CommonLayout {
  fun executableDocumentPackageName(filePath: String?): String
  fun operationName(name: String, capitalizedOperationType: String): String
  fun fragmentName(name: String): String
}

interface SchemaAndOperationsLayout : SchemaLayout, OperationsLayout
interface ExecutableSchemaLayout : SchemaLayout
