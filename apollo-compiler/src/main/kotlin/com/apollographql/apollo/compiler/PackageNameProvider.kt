package com.apollographql.apollo.compiler

import java.io.File

class PackageNameProvider(
    /**
     * the root package name for all classes:
     *
     * fragments will use rootPackageName.fragment
     * types will use rootPackageName.type
     * operations, will use rootPackageName.{relativePosition to rootDir}
     */
    private val rootPackageName: String?,
    private val rootDir: File?,
    /**
     * the package name for fragments and types will be written there
     */
    val irPackageName: String,
    /**
     * the package name for fragments, types, queries and mutations
     * This will flatten all the classes in the same package name. If you have operations in subfolders,
     * use rootPackageName instead
     */
    private val outputPackageName: String?
) {
  fun fragmentsPackageName(): String {
    return if (irPackageName.isNotEmpty()) "$irPackageName.fragment" else "fragment"
  }

  fun typesPackageName(): String {
    return if (irPackageName.isNotEmpty()) "$irPackageName.type" else "type"
  }

  fun operationPackageName(filePath: String): String {
    return if (outputPackageName.isNullOrEmpty()) {
      filePath.formatPackageName()
    } else {
      outputPackageName
    }
  }
}