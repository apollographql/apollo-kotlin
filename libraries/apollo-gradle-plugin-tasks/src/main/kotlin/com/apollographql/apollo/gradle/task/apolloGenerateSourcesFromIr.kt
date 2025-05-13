package com.apollographql.apollo.gradle.task

import com.apollographql.apollo.compiler.EntryPoints
import gratatouille.GAny
import gratatouille.GFileName
import gratatouille.GInputFile
import gratatouille.GInputFiles
import gratatouille.GLogger
import gratatouille.GOutputDirectory
import gratatouille.GOutputFile
import gratatouille.GTask


@GTask
fun apolloGenerateSourcesFromIr(
    logger: GLogger,
    arguments: Map<String, GAny?>,
    warnIfNotFound: Boolean,
    codegenSchemas: GInputFiles,
    irOperations: GInputFile,
    usedCoordinates: GInputFile,
    upstreamMetadata: GInputFiles,
    codegenOptions: GInputFile,
    // outputs
    @GFileName("operationManifest.json") operationManifest: GOutputFile,
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


