package com.apollographql.apollo3.compiler

import java.io.File

/**
 * A [PackageNameGenerator] computes the package name for a given file. Files can be either
 * - executable files containing operations and fragments
 * - schema files containing type definitions or introspection json
 */
interface PackageNameGenerator {
  /**
   * This will be called with
   * - the executable filePath for operations and fragments
   * - the main schema filePath for everything else
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

  class Flat(private val packageName: String): PackageNameGenerator {
    override fun packageName(filePath: String): String {
      return packageName
    }

    override val version: String
      get() = error("this should only be called from the Gradle Plugin")
  }
}


/**
 * A helper class to get a package name from a list of root folders
 *
 * Given:
 * - a list of root folders like "src/main/graphql/", "src/debug/graphql/", etc...
 * - an absolute file path like "/User/lee/projects/app/src/main/graphql/com/example/Query.graphql
 * will return "com.example"
 */
private fun relativeToRoots(roots: Set<String>, filePath: String): String {
  val file = File(File(filePath).absolutePath).normalize()
  roots.map { File(it).normalize() }.forEach { sourceDir ->
    try {
      val relative = file.toRelativeString(sourceDir)
      if (relative.startsWith(".."))
        return@forEach

      return relative
    } catch (e: IllegalArgumentException) {

    }
  }
  // Be robust if the file is not found.
  // This is for compatibility reasons mainly.
  return ""
}

/**
 * Return the packageName if this file is in these roots or throw else
 */
internal fun filePackageName(roots: Set<String>, filePath: String): String {
  val relative = relativeToRoots(roots, filePath)

  return relative
      .split(File.separator)
      .filter { it.isNotBlank() }
      .dropLast(1)
      .joinToString(".")
}

internal fun packageNameGenerator(
    packageName: String?,
    packageNamesFromFilePaths: Boolean?,
    packageNameRoots: Set<String>?,
): PackageNameGenerator {
  return when {
    packageName != null -> PackageNameGenerator.Flat(packageName)
    packageNamesFromFilePaths == true -> {
      check(packageNameRoots != null) {
        "packageNameRoots is required to use packageNamesFromFilePaths"
      }
      object : PackageNameGenerator {
        override fun packageName(filePath: String): String {
          return filePackageName(packageNameRoots, filePath)
        }

        override val version: String
          get() = "unused"

      }
    }
    else -> error("Apollo: packageName is required")
  }
}