package com.apollographql.apollo.gradle.api

import org.gradle.api.Action
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Provider

interface CompilationUnit {
  val name: String
  val serviceName: String
  val variantName: String
  val androidVariant: Any?

  var compilerParams: CompilerParams

  val outputDir: Provider<Directory>
  val transformedQueriesDir: Provider<Directory>

  fun configure(configure: Action<Params>)
  fun setSources(rootFolder: Provider<Directory>)

  data class Params(
      val graphqlFolder: DirectoryProperty,
      val schemaFile: RegularFileProperty,
      val rootPackageName: Provider<String>
  )
}
