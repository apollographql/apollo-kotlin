package com.apollographql.apollo.compiler.codegen

interface CommonLayout {
  fun className(name: String): String
  fun propertyName(name: String): String
}

interface SchemaLayout : CommonLayout {
  fun schemaPackageName(): String
  fun schemaTypeName(schemaTypeName: String): String
  fun schemaName(): String
  fun assertionsName(): String
  fun paginationName(): String
}

interface OperationsLayout: CommonLayout {
  fun executableDocumentPackageName(filePath: String?): String
  fun operationName(name: String, capitalizedOperationType: String): String
  fun fragmentName(name: String): String
}

interface SchemaAndOperationsLayout : SchemaLayout, OperationsLayout
