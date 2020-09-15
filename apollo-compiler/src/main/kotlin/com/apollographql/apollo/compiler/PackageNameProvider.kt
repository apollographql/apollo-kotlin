package com.apollographql.apollo.compiler

interface PackageNameProvider {
  fun operationPackageName(filePath: String): String
}
