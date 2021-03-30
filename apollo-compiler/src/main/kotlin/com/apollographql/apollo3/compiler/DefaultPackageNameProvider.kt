package com.apollographql.apollo3.compiler

class DefaultPackageNameProvider constructor(
    private val rootPackageName: String,
    private val roots: Roots,
) : PackageNameProvider {

  override fun operationPackageName(filePath: String) = filePackageName(filePath).withRootPackageName()

  private fun String.withRootPackageName() = "$rootPackageName.$this".removePrefix(".")
  private fun filePackageName(filePath: String) = roots.filePackageName(filePath).removeSuffix(".")
}


