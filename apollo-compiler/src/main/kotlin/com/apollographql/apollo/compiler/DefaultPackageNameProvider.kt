package com.apollographql.apollo.compiler

import java.io.File

class DefaultPackageNameProvider constructor(
    private val rootPackageName: String,
    private val schemaPackageName: String,
    private val roots: Roots,
    private val useFilePackageNameForFragments: Boolean
) : PackageNameProvider {

  override fun operationPackageName(filePath: String): String {
    return "$rootPackageName.${roots.filePackageName(filePath)}".removePrefix(".").removeSuffix(".")
  }

  override fun fragmentPackageName(filePath: String): String {
    return if (useFilePackageNameForFragments) {
      operationPackageName(filePath).removePrefix(".")
    } else {
      "$schemaPackageName.fragment".removePrefix(".")
    }
  }
}


