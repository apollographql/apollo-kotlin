package com.apollographql.apollo.gradle.api

import org.gradle.api.Action
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Provider
import java.io.File

interface CompilationUnit {
  val name: String
  val serviceName: String
  val variantName: String
  val androidVariant: Any?
  val compilerParams: CompilerParams
  val outputDir: Provider<Directory>
  val transformedQueriesDir: Provider<Directory>

  fun compilerParams(action: Action<CompilerParams>)
  fun sources(action: Action<Sources>)

  class Sources(
      val schemaFile: RegularFileProperty,
      val graphqlDir: DirectoryProperty
  ) {
    fun schemaFile(schemaFile: File) {
      this.schemaFile.set(schemaFile)
    }

    fun graphqlDir(graphqlDir: File) {
      require(graphqlDir.isDirectory) { "graphqlDir must be a directory: ${graphqlDir.absolutePath}"}
      this.graphqlDir.set(graphqlDir)
    }

    fun graphqlDir(graphqlDir: Provider<out Directory>) {
      this.graphqlDir.set(graphqlDir)
    }
  }
}
