package com.apollographql.apollo.compiler

import java.io.File

class DefaultPackageNameProvider constructor(val roots: Roots, schemaPackageName: String, private val rootPackageName: String) : PackageNameProvider {

  override val fragmentsPackageName = rootPackageName.appendPackageName(schemaPackageName).appendPackageName("fragment")
  override val typesPackageName = rootPackageName.appendPackageName(schemaPackageName).appendPackageName("type")

  override fun operationPackageName(filePath: String): String {
    return rootPackageName.appendPackageName(roots.filePackageName(filePath))
  }

  companion object {
    fun of(rootFolders: Collection<File>, schemaPackageName: String, rootPackageName: String): DefaultPackageNameProvider {
      return DefaultPackageNameProvider(
          Roots(rootFolders),
          schemaPackageName,
          rootPackageName)
    }
  }
}

internal fun String.appendPackageName(packageName: String) = "$this.$packageName".removePrefix(".").removeSuffix(".")

