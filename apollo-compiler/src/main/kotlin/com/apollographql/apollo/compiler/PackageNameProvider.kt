package com.apollographql.apollo.compiler

interface PackageNameProvider {
  val fragmentsPackageName: String
  val typesPackageName: String
  fun operationPackageName(filePath: String): String
}
