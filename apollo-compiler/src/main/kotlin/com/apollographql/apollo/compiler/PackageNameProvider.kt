package com.apollographql.apollo.compiler

class PackageNameProvider(
    /**
     * Location of the schema file.
     *
     * Used to format target package name if custom root package name is not provided
     * or to format relative position for GraphQL operations.
     */
    private val schemaFilePath: String,
    /**
     * Root package name for all classes:
     *
     * - fragments will use `rootPackageName.fragment`
     * - types will use `rootPackageName.type`
     * - operations, will use `rootPackageName.{relativePosition to schema location}`
     */
    rootPackageName: String? = null
) {
  val packageName: String = rootPackageName?.takeIf { it.isNotEmpty() } ?: schemaFilePath.formatPackageName()
  val fragmentsPackageName: String = "$packageName.fragment".removePrefix(".")
  val typesPackageName: String = "$packageName.type".removePrefix(".")

  fun operationPackageName(operationFilePath: String): String {
    val relativePackageName = operationFilePath.formatPackageName()
        .removePrefix(schemaFilePath.formatPackageName())
        .removePrefix(".")
        .removePrefix(packageName)
        .removePrefix(".")
    return "$packageName.$relativePackageName".removePrefix(".").removeSuffix(".")
  }
}
