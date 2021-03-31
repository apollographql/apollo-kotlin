package com.apollographql.apollo3.compiler

/**
 * A [PackageNameProvider] that puts all fragments in the same folder
 * @param typePackageName: the typePackageName, including the rootPackageName
 */
class DefaultPackageNameProvider constructor(
    private val typePackageName: String,
    private val rootPackageName: String,
    private val roots: Roots,
) : PackageNameProvider {

  override fun operationPackageName(filePath: String) = filePackageName(filePath).withRootPackageName()
  override fun fragmentPackageName(filePath: String) = typePackageName

  private fun String.withRootPackageName() = "$rootPackageName.$this".removePrefix(".")
  private fun filePackageName(filePath: String) = roots.filePackageName(filePath).removeSuffix(".")
}


