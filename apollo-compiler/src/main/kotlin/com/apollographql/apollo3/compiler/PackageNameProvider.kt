package com.apollographql.apollo3.compiler

interface PackageNameProvider {
  fun operationPackageName(filePath: String): String

  fun fragmentPackageName(filePath: String): String

  fun inputObjectPackageName(name: String): String
  fun customScalarsPackageName(): String
  fun enumPackageName(name: String): String
}
