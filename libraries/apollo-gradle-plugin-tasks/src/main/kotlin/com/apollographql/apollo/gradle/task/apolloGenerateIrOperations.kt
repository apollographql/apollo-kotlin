package com.apollographql.apollo.gradle.task

import com.apollographql.apollo.compiler.EntryPoints
import gratatouille.tasks.GAny
import gratatouille.tasks.GInputFile
import gratatouille.tasks.GInputFiles
import gratatouille.tasks.GLogger
import gratatouille.tasks.GOutputFile
import gratatouille.tasks.GTask

@GTask
internal fun apolloGenerateIrOperations(
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