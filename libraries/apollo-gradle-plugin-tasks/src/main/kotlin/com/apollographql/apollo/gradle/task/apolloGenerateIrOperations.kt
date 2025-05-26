package com.apollographql.apollo.gradle.task

import com.apollographql.apollo.compiler.EntryPoints
import gratatouille.GAny
import gratatouille.GInputFile
import gratatouille.GInputFiles
import gratatouille.GLogger
import gratatouille.GOutputFile
import gratatouille.GTask

@GTask
fun apolloGenerateIrOperations(
    logger: GLogger,
    arguments: Map<String, GAny?>,
    warnIfNotFound: Boolean,
    codegenSchemas: GInputFiles,
    graphqlFiles: GInputFiles,
    upstreamIrFiles: GInputFiles,
    irOptionsFile: GInputFile,
    irOperationsFile: GOutputFile,
) {
  EntryPoints.buildIr(
      logger = logger.asLogger(),
      arguments = arguments,
      warnIfNotFound = warnIfNotFound,
      graphqlFiles = graphqlFiles.toInputFiles(),
      codegenSchemaFiles = codegenSchemas.toInputFiles(),
      upstreamIrOperations = upstreamIrFiles.toInputFiles(),
      irOptionsFile = irOptionsFile,
      irOperationsFile = irOperationsFile,
  )
}