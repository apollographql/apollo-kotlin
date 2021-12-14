package com.apollographql.apollo3.compiler

import java.io.File

/**
 * A helper class to get a package name from a list of root folders
 *
 * Given:
 * - a list of root folders like "src/main/graphql/", "src/debug/graphql/", etc...
 * - an absolute file path like "/User/lee/projects/app/src/main/graphql/com/example/Query.graphql
 * will return "com.example"
 */
class Roots(rootFolders: Collection<File>) {
  private val roots = rootFolders.map { File(it.absolutePath).normalize() }

  private fun relativeToRoots(filePath: String): String {
    val file = File(File(filePath).absolutePath).normalize()
    roots.forEach { sourceDir ->
      try {
        val relative = file.toRelativeString(sourceDir)
        if (relative.startsWith(".."))
          return@forEach

        return relative
      } catch (e: IllegalArgumentException) {
      }
    }
    throw IllegalArgumentException("$filePath is not found in:\n${roots.joinToString("\n")}\n")
  }

  /**
   * Return the packageName if this file is in these [Roots] or throw else
   */
  fun filePackageName(filePath: String): String {
    val relative = relativeToRoots(filePath)

    return relative
        .split(File.separator)
        .filter { it.isNotBlank() }
        .dropLast(1)
        .joinToString(".")
  }
}