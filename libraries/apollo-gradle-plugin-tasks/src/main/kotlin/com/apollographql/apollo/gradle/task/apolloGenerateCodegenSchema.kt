package com.apollographql.apollo.gradle.task

import com.apollographql.apollo.compiler.EntryPoints
import gratatouille.GAny
import gratatouille.GInputFile
import gratatouille.GInputFiles
import gratatouille.GLogger
import gratatouille.GOutputFile
import gratatouille.GTask

@GTask
fun apolloGenerateCodegenSchema(
    logger: GLogger,
    arguments: Map<String, GAny?>,
    warnIfNotFound: Boolean,
    schemaFiles: GInputFiles,
    fallbackSchemaFiles: GInputFiles,
    upstreamSchemaFiles: GInputFiles,
    codegenSchemaOptionsFile: GInputFile,
    codegenSchemaFile: GOutputFile,
) {
  if (upstreamSchemaFiles.isNotEmpty()) {
    /**
     * Output an empty file
     */
    codegenSchemaFile.let {
      it.delete()
      it.createNewFile()
    }
    return
  }

  EntryPoints.buildCodegenSchema(
      logger = logger.asLogger(),
      arguments = arguments,
      warnIfNotFound = warnIfNotFound,
      normalizedSchemaFiles = (schemaFiles.takeIf { it.isNotEmpty() } ?: fallbackSchemaFiles).toInputFiles(),
      codegenSchemaOptionsFile = codegenSchemaOptionsFile,
      codegenSchemaFile = codegenSchemaFile,
  )
}
