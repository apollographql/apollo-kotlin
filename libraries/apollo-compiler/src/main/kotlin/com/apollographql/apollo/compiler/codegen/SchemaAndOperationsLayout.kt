package com.apollographql.apollo.compiler.codegen

import com.apollographql.apollo.annotations.ApolloExperimental

@ApolloExperimental
interface CommonLayout {
  fun className(name: String): String
  fun propertyName(name: String): String
}

@ApolloExperimental
interface SchemaLayout : CommonLayout {
  fun schemaPackageName(): String
  fun schemaTypeName(schemaTypeName: String): String
  fun schemaName(): String
  fun assertionsName(): String
  fun paginationName(): String
}

@ApolloExperimental
interface OperationsLayout: CommonLayout {
  fun executableDocumentPackageName(filePath: String?): String
  fun operationName(name: String, capitalizedOperationType: String): String
  fun fragmentName(name: String): String
}

@ApolloExperimental
interface SchemaAndOperationsLayout : SchemaLayout, OperationsLayout
