package com.apollographql.apollo.compiler

class PackageNameProvider(
    private val schemaFilePath: String,
    rootPackageName: String? = null
) {
  val packageName: String = rootPackageName?.takeIf { it.isNotEmpty() } ?: schemaFilePath.formatPackageName()
  val fragmentsPackageName: String = "$packageName.fragment"
  val typesPackageName: String = "$packageName.type"

  fun operationPackageName(operationFilePath: String): String {
    val relativePackageName = operationFilePath.formatPackageName()
        .replace(schemaFilePath.formatPackageName(), "")
        .replace("..", ".")
        .removePrefix(packageName)
        .removePrefix(".")
    return "$packageName.$relativePackageName".removeSuffix(".")
  }
}
