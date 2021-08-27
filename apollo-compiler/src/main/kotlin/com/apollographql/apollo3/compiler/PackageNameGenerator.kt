package com.apollographql.apollo3.compiler

/**
 * A [PackageNameGenerator] computes the package name for a given file. Files can be either
 * - executable files containing operations and fragments
 * - schema files containing type definitions or introspection json
 */
interface PackageNameGenerator {
  /**
   * This will be called with
   * - the executable filePath for operations
   * - the main schema filePath for everything else
   *
   * **Note**: Fragments are generated using the fixed schema package name and not the file
   * where they are defined. This is because multi-modules scenarios need to know where to
   * lookup fragments
   */
  fun packageName(filePath: String): String

  /**
   * A version that is used as input of the Gradle task. Since [PackageNameGenerator] cannot easily be serialized and influences
   * the output of the task, we need a way to mark the task out-of-date when the implementation changes.
   *
   * Two different implementations **must** have different versions.
   *
   * When using the compiler outside of a Gradle context, [version] is not used, making it the empty string is fine.
   */
  val version: String

  class Flat(val packageName: String) : PackageNameGenerator {
    override fun packageName(filePath: String) = packageName
    override val version: String
      get() = "Flat-$packageName"
  }

  class FilePathAware constructor(
      private val roots: Roots,
      private val rootPackageName: String = "",
  ) : PackageNameGenerator {

    override fun packageName(filePath: String): String {
      val p = try {
        roots.filePackageName(filePath).removeSuffix(".")
      } catch (e: Exception) {
        ""
      }

      return "$rootPackageName.$p".removePrefix(".")
    }

    override val version: String
      get() = "FilePathAware-$rootPackageName"
  }
}
