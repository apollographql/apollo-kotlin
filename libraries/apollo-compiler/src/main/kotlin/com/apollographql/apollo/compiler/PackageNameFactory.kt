package com.apollographql.apollo.compiler

import java.io.File

/**
 * A [PackageNameFactory] computes the package name for a given file. Files can be either
 * - executable files containing operations and fragments
 * - schema files containing type definitions or introspection json
 */
internal interface PackageNameFactory {
  /**
   * This will be called with
   * - the executable filePath for operations and fragments
   * - the main schema filePath for schema types
   * - the empty string if the schema and/or operations are not read from a [File]
   */
  fun packageName(filePath: String): String

  class Flat(private val packageName: String): PackageNameFactory {
    override fun packageName(filePath: String): String {
      return packageName
    }
  }

  class NormalizedPathAware(private val rootPackageName: String?): PackageNameFactory {
    override fun packageName(filePath: String): String {
      return "${rootPackageName}.${filePath.toPackageName()}".removePrefix(".")
    }
  }
}
