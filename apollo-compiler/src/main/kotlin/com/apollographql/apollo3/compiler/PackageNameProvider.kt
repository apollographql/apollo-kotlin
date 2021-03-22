package com.apollographql.apollo3.compiler

interface PackageNameProvider {
  fun operationPackageName(filePath: String): String
  fun operationVariablesAdapterPackageName(filePath: String): String
  fun operationResponseAdapterPackageName(filePath: String): String

  fun fragmentPackageName(filePath: String): String
  fun fragmentVariablesAdapterPackageName(filePath: String): String
  fun fragmentResponseAdapterPackageName(filePath: String): String

  fun inputObjectPackageName(name: String): String
  fun customScalarsPackageName(): String
  fun enumPackageName(name: String): String
  fun enumAdapterPackageName(name: String): String
}
