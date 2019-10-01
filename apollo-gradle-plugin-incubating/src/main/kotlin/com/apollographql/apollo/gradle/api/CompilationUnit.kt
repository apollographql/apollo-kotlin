package com.apollographql.apollo.gradle.api

import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider

interface CompilationUnit {
  val serviceName: String
  val variantName: String
  val androidVariant: Any?
  val outputDir: Provider<Directory>
  val transformedQueriesDir: Provider<Directory>
}