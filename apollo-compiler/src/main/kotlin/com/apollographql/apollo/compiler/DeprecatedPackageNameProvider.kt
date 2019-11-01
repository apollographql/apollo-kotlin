package com.apollographql.apollo.compiler

import java.io.File

class DeprecatedPackageNameProvider(
    private val rootPackageName: String,

    /**
     * the package name for fragments and types will be written there
     */
    val schemaPackageName: String,

    /**
     * the package name for fragments, types, queries and mutations
     * This will flatten all the classes in the same package name. If you have operations in subfolders,
     * use rootPackageName instead
     */
    @Deprecated("use rootPackageName instead")
    private val outputPackageName: String?
) : PackageNameProvider {
  override val fragmentsPackageName = outputPackageName?.appendPackageName("fragment") ?: rootPackageName
      .appendPackageName(schemaPackageName)
      .appendPackageName("fragment")

  override val typesPackageName = outputPackageName?.appendPackageName("type") ?: rootPackageName
      .appendPackageName(schemaPackageName)
      .appendPackageName("type")

  override fun operationPackageName(filePath: String): String {
    if (outputPackageName != null) {
      return outputPackageName
    }

    val packageName = filePath.formatPackageName(dropLast = 1)
    if (packageName == null) {
      throw IllegalArgumentException("graphql file must be placed under src/{foo}/graphql:\n$filePath")
    }
    return rootPackageName.appendPackageName(packageName)
  }
}

fun String.appendPackageName(packageName: String) = "$this.$packageName".removePrefix(".").removeSuffix(".")

fun File.child(vararg path: String) = File(this, path.toList().joinToString(File.separator))

fun String.relativePathToGraphql(dropLast: Int = 0): String? {
  val parts = split(File.separator)
      .filter { it.isNotBlank() }
      .dropLast(dropLast)

  for (i in 2 until parts.size) {
    if (parts[i - 2] == "src" && parts[i] == "graphql") {
      return parts.subList(i + 1, parts.size).joinToString(File.separator)
    }
  }

  return null
}

fun String.formatPackageName(dropLast: Int = 0): String? {
  return relativePathToGraphql(dropLast = dropLast)?.split(File.separator)?.joinToString(".")
}


