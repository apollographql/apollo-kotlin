package com.apollographql.apollo.compiler

class PackageNameProvider(
    private val schemaFilePath: String,
    customPackageName: String? = null
) {
  val packageName: String = customPackageName?.takeIf { it.isNotEmpty() } ?: schemaFilePath.formatPackageName()
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
