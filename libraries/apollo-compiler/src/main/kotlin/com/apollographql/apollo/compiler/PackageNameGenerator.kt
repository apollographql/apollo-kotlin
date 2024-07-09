package com.apollographql.apollo.compiler

import com.apollographql.apollo.annotations.ApolloDeprecatedSince
import java.io.File

/**
 * A [PackageNameGenerator] computes the package name for a given file. Files can be either
 * - executable files containing operations and fragments
 * - schema files containing type definitions or introspection json
 */
@Suppress("DEPRECATION")
@Deprecated("Use ApolloCompilerPlugin.layout() instead. See https://go.apollo.dev/ak-compiler-plugins for more details.")
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
interface PackageNameGenerator {
  /**
   * This will be called with
   * - the executable filePath for operations and fragments
   * - the main schema filePath for schema types
   * - the empty string if the schema and/or operations are not read from a [File]
   */
  fun packageName(filePath: String): String

  /**
   * A version that is used as input of the Gradle task. Since [PackageNameGenerator] cannot easily be serialized and influences
   * the output of the task, we need a way to mark the task out-of-date when the implementation changes.
   *
   * Two different implementations **must** have different versions.
   *
   * When using the compiler outside a Gradle context, [version] is not used, making it the empty string is fine.
   */
  val version: String
    get() = error("PackageNameGenerator implementations must override `version`.")

  class Flat(private val packageName: String): PackageNameGenerator {
    override fun packageName(filePath: String): String {
      return packageName
    }
  }

  class NormalizedPathAware(private val rootPackageName: String?): PackageNameGenerator {
    override fun packageName(filePath: String): String {
      return "${rootPackageName}.${filePath.toPackageName()}".removePrefix(".")
    }
  }
}
