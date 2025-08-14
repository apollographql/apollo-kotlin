package com.apollographql.apollo.gradle.task

import com.apollographql.apollo.compiler.model.CompilationUnitModel
import com.apollographql.apollo.compiler.model.writeTo
import gratatouille.tasks.GOutputFile
import gratatouille.tasks.GTask

@GTask
internal fun apolloGenerateCompilationUnitModel(
    // Inputs
    gradleProjectPath: String,
    serviceName: String,
    schemaFiles: Set<String>,
    graphqlSrcDirs: Set<String>,
    upstreamGradleProjectPaths: Set<String>,
    downstreamGradleProjectPaths: Set<String>,
    endpointUrl: String?,
    endpointHeaders: Map<String, String>?,
    pluginDependencies: Set<String>,

    // Outputs
    compilationUnitModel: GOutputFile,
) {
  CompilationUnitModel(
      gradleProjectPath = gradleProjectPath,
      serviceName = serviceName,
      schemaFiles = schemaFiles,
      graphqlSrcDirs = graphqlSrcDirs,
      upstreamGradleProjectPaths = upstreamGradleProjectPaths,
      downstreamGradleProjectPaths = downstreamGradleProjectPaths,
      endpointUrl = endpointUrl,
      endpointHeaders = endpointHeaders,
      pluginDependencies = pluginDependencies,
  )
      .writeTo(compilationUnitModel)
}
