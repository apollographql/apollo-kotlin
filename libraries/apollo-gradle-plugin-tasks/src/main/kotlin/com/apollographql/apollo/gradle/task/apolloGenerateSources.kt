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
fun apolloGenerateSources(
    logger: GLogger,
    arguments: Map<String, GAny?>,
    warnIfNotFound: Boolean,
    schemas: GInputFiles,
    fallbackSchemas: GInputFiles,
    executableDocuments: GInputFiles,
    codegenSchemaOptions: GInputFile,
    codegenOptions: GInputFile,
    irOptions: GInputFile,
    @GFileName("operationManifest.json") operationManifest: GOutputFile,
    outputDirectory: GOutputDirectory,
    dataBuildersOutputDirectory: GOutputDirectory,
) {
  EntryPoints.buildSources(
      arguments = arguments,
      warnIfNotFound = warnIfNotFound,
      logger = logger.asLogger(),
      schemas = (schemas.takeIf { it.isNotEmpty() } ?: fallbackSchemas).toInputFiles(),
      executableDocuments = executableDocuments.toInputFiles(),
      codegenSchemaOptions = codegenSchemaOptions,
      codegenOptions = codegenOptions,
      irOptions = irOptions,
      operationManifest = operationManifest,
      outputDirectory = outputDirectory,
      dataBuildersOutputDirectory = dataBuildersOutputDirectory,
  )
}