package com.apollographql.apollo3.compiler

interface PackageNameProvider {
  fun operationPackageName(filePath: String): String
}
