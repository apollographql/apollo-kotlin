package com.apollographql.apollo.gradle.task

import com.apollographql.apollo.compiler.EntryPoints
import gratatouille.GAny
import gratatouille.GInputFile
import gratatouille.GInputFiles
import gratatouille.GLogger
import gratatouille.GOutputDirectory
import gratatouille.GTask

@GTask
fun apolloGenerateDataBuildersSources(
    logger: GLogger,
    arguments: Map<String, GAny?>,
    warnIfNotFound: Boolean,
    codegenSchemas: GInputFiles,
    downstreamUsedCoordinates: GInputFile,
    upstreamMetadata: GInputFiles,
    codegenOptions: GInputFile,
    outputDirectory: GOutputDirectory,
) {
  EntryPoints.buildDataBuilders(
      arguments = arguments,
      logger = logger.asLogger(),
      warnIfNotFound = warnIfNotFound,
      codegenSchemas = codegenSchemas.toInputFiles(),
      upstreamMetadatas = upstreamMetadata.toInputFiles(),
      downstreamUsedCoordinates = downstreamUsedCoordinates,
      codegenOptions = codegenOptions,
      outputDirectory = outputDirectory,
  )
}
