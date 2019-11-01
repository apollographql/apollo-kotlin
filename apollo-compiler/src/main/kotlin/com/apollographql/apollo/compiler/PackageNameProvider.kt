package com.apollographql.apollo.compiler

import java.io.File

interface PackageNameProvider {
  val fragmentsPackageName: String
  val typesPackageName: String
  fun operationPackageName(filePath: String): String
}

fun String.toPackageName() = split(File.separator)
    .filter { it.isNotBlank() }
    .joinToString(".")
