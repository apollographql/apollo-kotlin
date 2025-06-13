package com.apollographql.apollo.gradle.task

import com.apollographql.apollo.compiler.EntryPoints
import gratatouille.tasks.GAny
import gratatouille.tasks.GInputFile
import gratatouille.tasks.GInputFiles
import gratatouille.tasks.GLogger
import gratatouille.tasks.GOutputFile
import gratatouille.tasks.GTask

@GTask
internal fun apolloGenerateCodegenSchema(
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
