package com.apollographql.apollo.compiler

import com.apollographql.apollo.compiler.ir.Fragment
import com.apollographql.apollo.compiler.parser.introspection.IntrospectionQuery
import com.squareup.moshi.JsonClass
import java.io.File

@JsonClass(generateAdapter = true)
data class ApolloMetadata(
    val schema: IntrospectionQuery.Wrapper?,
    /**
     * The fragments, in IR format
     */
    val fragments: List<Fragment>,
    /**
     * The generated input objects, enums
     */
    val types: Set<String>,
    val schemaPackageName: String?,
    /**
     * The module name, for debug
     */
    val moduleName: String,
    val generateKotlinModels: Boolean,
    val pluginVersion: String,
    val customTypesMap: Map<String, String>
) {

  fun withResolvedFragments(projectDir: File): ApolloMetadata {
    return copy(
        fragments = fragments.map {
          // Try to lookup the file in the rootProject if it exists
          if (File(it.filePath).isAbsolute) {
            // already absolute, build cache will not work
            return@map it
          }

          val resolvedPath = File(projectDir, it.filePath)
          if (resolvedPath.exists()) {
            return@map it.copy(
                filePath = resolvedPath.absolutePath
            )
          }

          // default: best effort mode, the error will not be able to display the context
          it
        }
    )
  }

  fun withRelativeFragments(rootProjectDir: File): ApolloMetadata {
    return copy(
        fragments = fragments.map {
          // Remove absolute paths from the artifacts in order to not break gradle build cache
          val relativePath = try {
            File(it.filePath).relativeTo(rootProjectDir.absoluteFile).path
          } catch (e: IllegalArgumentException) {
            println("Apollo: ${it.filePath} and $rootProjectDir don't share the same root, build cache will not work")
            // fallback to absolute
            it.filePath
          }
          it.copy(filePath = relativePath)
        }
    )
  }

  fun writeTo(file: File) {
    this.toJson(file)
  }

  companion object {
    fun List<ApolloMetadata>.merge(): ApolloMetadata? {
      if (isEmpty()) {
        return null
      }
      // ensure a single schema
      val rootMetadataList = filter { it.schema != null }
      check(rootMetadataList.size <= 1) {
        "Apollo: A schema is defined in multiple modules: ${rootMetadataList.map { it.moduleName }.joinToString(", ")}.\n" +
            "There should be only one root module defining the schema, check your dependencies."
      }
      check(rootMetadataList.isNotEmpty()) {
        "Apollo: Cannot find a schema in parent modules. Searched in ${map { it.moduleName }.joinToString(", ")}"
      }
      val rootMetadata = rootMetadataList.first()

      // ensure the same schemaPackageName
      map { it.schemaPackageName }.filterNotNull().distinct().let {
        check(it.size == 1) {
          "Apollo: All modules should have the same schemaPackageName. Found:" + it.joinToString(", ")
        }
      }

      // ensure the same generateKotlinModels
      map { it.generateKotlinModels }.distinct().let {
        check(it.size == 1) {
          "Apollo: All modules should have the same generateKotlinModels. Found:" + it.joinToString(", ")
        }
      }

      // ensure the same pluginVersion
      map { it.pluginVersion }.distinct().let {
        check(it.size == 1) {
          "Apollo: All modules should be generated with the same apollo version. Found:" + it.joinToString(", ")
        }
      }

      // no need to validate distinct fragment names, this will be done later when aggregating the Fragments
      return rootMetadata.copy(
          fragments = flatMap { it.fragments },
          moduleName = "*",
          types = flatMap { it.types }.toSet(),
      )
    }

    fun readFrom(file: File): ApolloMetadata? {
      return try {
        file.fromJson()
      } catch (e: Exception) {
        null
      }
    }
  }
}
