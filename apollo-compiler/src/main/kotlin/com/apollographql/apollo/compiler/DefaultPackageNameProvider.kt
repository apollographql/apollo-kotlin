package com.apollographql.apollo.compiler

import java.io.File

class DefaultPackageNameProvider constructor(
    private val rootPackageName: String,
    private val schemaPackageName: String,
    private val roots: Roots,
    private val packageName: String?
) : PackageNameProvider {

  override fun operationPackageName(filePath: String): String {
    return packageName ?: "$rootPackageName.${roots.filePackageName(filePath)}".removePrefix(".").removeSuffix(".")
  }

  override fun fragmentPackageName(filePath: String): String {
    return if (packageName != null) {
      "$packageName.fragment".removePrefix(".")
    } else {
      "$schemaPackageName.fragment".removePrefix(".")
    }
  }
}


