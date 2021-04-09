package com.apollographql.apollo3.compiler

class DefaultPackageNameProvider constructor(
    private val rootPackageName: String,
    private val roots: Roots,
    // with the rootPackageName already included
    private val schemaPackageName: String
) : PackageNameProvider {

  override fun operationPackageName(filePath: String) = filePackageName(filePath)

  override fun fragmentPackageName(filePath: String) = filePackageName(filePath)

  override fun inputObjectPackageName(name: String) = "$schemaPackageName.type"
  override fun customScalarsPackageName() = "$schemaPackageName.type"
  override fun enumPackageName(name: String) = "$schemaPackageName.type"

  private fun filePackageName(filePath: String) = "$rootPackageName.${roots.filePackageName(filePath)}".removePrefix(".").removeSuffix(".")
}


