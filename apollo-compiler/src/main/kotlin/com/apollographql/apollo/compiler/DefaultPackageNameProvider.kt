package com.apollographql.apollo.compiler

class DefaultPackageNameProvider constructor(private val rootPackageName: String, private val roots: Roots) : PackageNameProvider {

  override fun operationPackageName(filePath: String): String {
    return "$rootPackageName.${roots.filePackageName(filePath)}".removePrefix(".").removeSuffix(".")
  }
}


