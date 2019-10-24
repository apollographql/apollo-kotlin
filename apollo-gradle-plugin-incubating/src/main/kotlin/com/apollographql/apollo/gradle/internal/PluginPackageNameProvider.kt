package com.apollographql.apollo.gradle.internal

import com.apollographql.apollo.compiler.PackageNameProvider
import com.apollographql.apollo.compiler.appendPackageName
import org.gradle.api.file.SourceDirectorySet
import java.io.File

class PluginPackageNameProvider(rootFolders: Collection<String>, schemaFile: File, private val rootPackageName: String) : PackageNameProvider {
  private val roots = rootFolders.map(::File)
  private val schemaPackageName = try {
    filePackageName(schemaFile.absolutePath)
  } catch (e: IllegalArgumentException) {
    // Can happen if the schema is not in the roots
    ""
  }

  override val fragmentsPackageName = rootPackageName.appendPackageName(schemaPackageName).appendPackageName("fragment")
  override val typesPackageName = rootPackageName.appendPackageName(schemaPackageName).appendPackageName("type")

  override fun operationPackageName(filePath: String): String {
    return rootPackageName.appendPackageName(filePackageName(filePath))
  }

  fun filePackageName(filePath: String): String {
    val file = File(filePath)
    roots.forEach { sourceDir ->
      try {
        val relative = file.toRelativeString(sourceDir)
        if (relative.startsWith(".."))
          return@forEach

        return relative
            .split(File.separator)
            .filter { it.isNotBlank() }
            .dropLast(1)
            .joinToString(".")
      } catch (e: IllegalArgumentException) {
      }
    }
    throw IllegalArgumentException("$filePath is not found in:\n${roots.joinToString("\n")}\n")
  }
}