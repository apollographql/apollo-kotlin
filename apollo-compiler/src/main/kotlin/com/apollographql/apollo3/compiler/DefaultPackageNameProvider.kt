package com.apollographql.apollo3.compiler

/**
 * A [PackageNameProvider] that puts all fragments in the same folder
 * @param schemaPackageName: the schemaPackageName, including the rootPackageName as it might come from metadata
 */
class DefaultPackageNameProvider constructor(
    private val schemaPackageName: String,
    private val rootPackageName: String,
    private val roots: Roots,
) : PackageNameProvider {

  override fun operationPackageName(filePath: String) = filePackageName(filePath).withRootPackageName()
  override fun fragmentPackageName(filePath: String) = "$schemaPackageName.fragment"
  override fun typePackageName() = "$schemaPackageName.type"

  private fun String.withRootPackageName() = "$rootPackageName.$this".removePrefix(".")
  private fun filePackageName(filePath: String) = roots.filePackageName(filePath).removeSuffix(".")
}
