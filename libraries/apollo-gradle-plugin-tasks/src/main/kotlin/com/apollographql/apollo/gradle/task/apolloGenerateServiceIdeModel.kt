package com.apollographql.apollo.gradle.task

import com.apollographql.apollo.compiler.ide.ServiceIdeModel
import com.apollographql.apollo.compiler.ide.writeTo
import gratatouille.tasks.GManuallyWired
import gratatouille.tasks.GOutputFile
import gratatouille.tasks.GTask

@GTask
internal fun apolloGenerateServiceIdeModel(
    // Inputs
    projectPath: String,
    serviceName: String,
    schemaFiles: Set<String>,
    graphqlSrcDirs: Set<String>,
    upstreamProjectPaths: Set<String>,
    downstreamProjectPaths: Set<String>,
    endpointUrl: String?,
    endpointHeaders: Map<String, String>?,
    useSemanticNaming: Boolean,

    // Outputs
    @GManuallyWired
    serviceIdeModelFile: GOutputFile,
) {
  ServiceIdeModel(
      projectPath = projectPath,
      serviceName = serviceName,
      schemaFiles = schemaFiles,
      graphqlSrcDirs = graphqlSrcDirs,
      upstreamProjectPaths = upstreamProjectPaths,
      downstreamProjectPaths = downstreamProjectPaths,
      endpointUrl = endpointUrl,
      endpointHeaders = endpointHeaders,
      useSemanticNaming = useSemanticNaming,
  )
      .writeTo(serviceIdeModelFile)
}

