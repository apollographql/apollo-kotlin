package com.apollographql.apollo.compiler

import java.io.File

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

  fun filePackageName(filePath: String): String {
    val relative = relativeToRoots(filePath)

    return relative
        .split(File.separator)
        .filter { it.isNotBlank() }
        .dropLast(1)
        .joinToString(".")
  }
}