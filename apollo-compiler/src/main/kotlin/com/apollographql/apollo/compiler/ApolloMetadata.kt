package com.apollographql.apollo.compiler

import com.apollographql.apollo.compiler.ir.Fragment
import com.apollographql.apollo.compiler.parser.introspection.IntrospectionSchema
import com.apollographql.apollo.compiler.parser.introspection.IntrospectionSchema.Companion.wrap
import com.squareup.moshi.JsonClass
import java.io.File
import java.util.zip.ZipFile

data class ApolloMetadata(
    val schema: IntrospectionSchema?,
    val options: Options,
    val fragments: List<Fragment>
) {
  @JsonClass(generateAdapter = true)
  data class Options(
      val schemaPackageName: String?,
      val moduleName: String,
      val generatedTypes: Set<String>
  )

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

  fun writeTo(dir: File) {
    dir.deleteRecursively()
    dir.mkdirs()
    schema?.wrap().toJson(dir.schema)
    options.toJson(dir.options)
    fragments.toJson(dir.fragments)
  }

  companion object {
    private val File.schema
      get() = File(this, "schema.json")
    private val File.options
      get() = File(this, "options.json")
    private val File.fragments
      get() = File(this, "fragments.json")

    fun List<ApolloMetadata>.merge(): ApolloMetadata? {
      if (isEmpty()) {
        return null
      }
      // ensure a single schema
      val rootMetadataList = filter { it.schema != null }
      check(rootMetadataList.size <= 1) {
        "Apollo: A schema is define in multiple modules: ${rootMetadataList.map { it.options.moduleName }.joinToString(", ")}.\n" +
            "There should be only one root module defining the schema, check your dependencies."
      }
      check(rootMetadataList.isNotEmpty()) {
        "Apollo: Cannot find a schema in parent modules. Searched in ${map { it.options.moduleName }.joinToString(", ")}"
      }
      val rootMetadata = rootMetadataList.first()

      // ensure the same schemaPackageName
      map { it.options.schemaPackageName }.filterNotNull().distinct().let {
        check(it.size == 1) {
          "Apollo: All modules should have the same schemaPackageName. Found:" + it.joinToString(", ")
        }
      }

      // no need to validate distinct fragment names, this will be done later when aggregating the Fragments
      return ApolloMetadata(
          schema = rootMetadataList.first().schema!!,
          fragments = flatMap { it.fragments },
          options = rootMetadata.options.copy(
              moduleName = "*",
              generatedTypes = flatMap { it.options.generatedTypes }.toSet()
          )
      )
    }

    fun readFromZip(zip: File): ApolloMetadata {
      check(zip.exists()) {
        "Apollo: Cannot find ${zip}, make sure to define apollo { generateApolloMetadata.set(true) } in the parent module"
      }

      val zipFile = ZipFile(zip)

      val schema = zipFile.getEntry("metadata/schema.json")?.let {
        IntrospectionSchema(zipFile.getInputStream(it), "from metadata/schema.json")
      }
      val options = zipFile.getEntry("metadata/options.json")!!.let {
        zipFile.getInputStream(it).fromJson<Options>()
      }
      val fragments = zipFile.getEntry("metadata/fragments.json")!!.let {
        zipFile.getInputStream(it).fromJsonList<Fragment>()
      }

      return ApolloMetadata(
          schema = schema,
          options = options,
          fragments = fragments
      )
    }

    fun readFromDirectory(dir: File): ApolloMetadata {
      check(dir.exists()) {
        "Apollo: Cannot find ${dir}, make sure to define apollo { generateApolloMetadata.set(true) } in the parent module"
      }

      val schema = dir.schema.takeIf { it.exists() } ?.let {
        IntrospectionSchema(it.inputStream(), "from $it")
      }
      val options = dir.options.inputStream().fromJson<Options>()
      val fragments = dir.fragments.inputStream().fromJsonList<Fragment>()

      return ApolloMetadata(
          schema = schema,
          options = options,
          fragments = fragments
      )
    }
  }
}
