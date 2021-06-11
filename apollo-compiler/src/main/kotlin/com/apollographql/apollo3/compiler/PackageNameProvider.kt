package com.apollographql.apollo3.compiler

interface PackageNameProvider {
  fun operationPackageName(filePath: String): String

  class Flat(val packageName: String) : PackageNameProvider {
    override fun operationPackageName(filePath: String) = packageName
  }

  class FilePathAware constructor(
      private val roots: Roots,
  ) : PackageNameProvider {

    override fun operationPackageName(filePath: String) = filePackageName(filePath)

    private fun filePackageName(filePath: String) = roots.filePackageName(filePath).removeSuffix(".")
  }
}
