package com.apollographql.apollo.gradle.task

import com.apollographql.apollo.compiler.EntryPoints
import gratatouille.tasks.GAny
import gratatouille.tasks.GInputFile
import gratatouille.tasks.GInputFiles
import gratatouille.tasks.GLogger
import gratatouille.tasks.GManuallyWired
import gratatouille.tasks.GOutputDirectory
import gratatouille.tasks.GOutputFile
import gratatouille.tasks.GTask


@GTask
internal fun apolloGenerateSourcesFromIr(
    logger: GLogger,
    arguments: Map<String, GAny?>,
    warnIfNotFound: Boolean,
    codegenSchemas: GInputFiles,
    irOperations: GInputFile,
    usedCoordinates: GInputFile,
    upstreamMetadata: GInputFiles,
    codegenOptions: GInputFile,
    // outputs
    @GManuallyWired
    operationManifest: GOutputFile,
    @GManuallyWired
    outputDirectory: GOutputDirectory,
    metadataOutput: GOutputFile,
) {
  EntryPoints.buildSourcesFromIr(
      logger = logger.asLogger(),
      arguments = arguments,
      warnIfNotFound = warnIfNotFound,
      codegenSchemas = codegenSchemas.toInputFiles(),
      irOperations = irOperations,
      usedCoordinates = usedCoordinates,
      upstreamMetadata = upstreamMetadata.toInputFiles(),
      codegenOptions = codegenOptions,
      operationManifest = operationManifest,
      outputDirectory = outputDirectory,
      metadataOutput = metadataOutput,
  )
}


