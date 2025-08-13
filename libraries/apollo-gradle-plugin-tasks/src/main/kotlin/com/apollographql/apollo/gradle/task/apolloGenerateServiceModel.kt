package com.apollographql.apollo.gradle.task

import com.apollographql.apollo.compiler.model.ServiceModel
import com.apollographql.apollo.compiler.model.writeTo
import gratatouille.tasks.GOutputFile
import gratatouille.tasks.GTask

@GTask
internal fun apolloGenerateServiceModel(
    // Inputs
    gradleProjectPath: String,
    serviceName: String,
    schemaFiles: Set<String>,
    graphqlSrcDirs: Set<String>,
    upstreamGradleProjectPaths: Set<String>,
    downstreamGradleProjectPaths: Set<String>,
    endpointUrl: String?,
    endpointHeaders: Map<String, String>?,
    telemetryUsedOptions: Set<String>,

    // Outputs
    serviceModelFile: GOutputFile,
) {
  ServiceModel(
      gradleProjectPath = gradleProjectPath,
      serviceName = serviceName,
      schemaFiles = schemaFiles,
      graphqlSrcDirs = graphqlSrcDirs,
      upstreamGradleProjectPaths = upstreamGradleProjectPaths,
      downstreamGradleProjectPaths = downstreamGradleProjectPaths,
      endpointUrl = endpointUrl,
      endpointHeaders = endpointHeaders,
      telemetryUsedOptions = telemetryUsedOptions,
  )
      .writeTo(serviceModelFile)
}

