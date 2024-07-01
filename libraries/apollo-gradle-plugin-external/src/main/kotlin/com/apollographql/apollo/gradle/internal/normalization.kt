package com.apollographql.apollo.gradle.internal

import java.io.File

/**
 * A helper class to get a package name from a list of root folders. This tries to mimic Gradle normalization behaviour for PathSensitivity.RELATIVE
 *
 * Given:
 * - a list of root folders like "src/main/graphql/", "src/debug/graphql/", etc...
 * - an absolute file path like "/User/lee/projects/app/src/main/graphql/com/example/Query.graphql
 * will return "com.example"
 *
 * See also https://github.com/gradle/gradle/issues/27836
 *
 */
internal fun File.normalizedPath(roots: Set<String>): String {
  val file = this.normalize()
  roots.map { File(it).normalize() }.forEach { sourceDir ->
    try {
      val relative = file.toRelativeString(sourceDir)
      if (relative.startsWith(".."))
        return@forEach

      // Make sure the normalized path is the same on all hosts
      return relative.replace(File.pathSeparatorChar, '/')
    } catch (e: IllegalArgumentException) {

    }
  }

  // Be robust if the file is not found.
  // This is for compatibility reasons mainly.
  return ""
}