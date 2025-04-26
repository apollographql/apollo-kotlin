package com.apollographql.apollo.gradle.internal

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.OutputDirectory

interface ApolloGenerateDataBuildersSourcesBaseTask {
  @get:OutputDirectory
  abstract val dataBuildersOutputDir: DirectoryProperty
}