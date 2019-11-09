package com.apollographql.apollo.gradle.api

import org.gradle.api.Action
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Provider
import java.io.File

interface CompilationUnit: CompilerParams {
  val name: String
  val serviceName: String
  val variantName: String
  val androidVariant: Any?

  val outputDir: Provider<Directory>
  val transformedQueriesDir: Provider<Directory>
}
