package com.apollographql.apollo.gradle.task

import com.apollographql.apollo.compiler.ApolloCompilerPlugin
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
internal fun apolloGenerateSources(
    logger: GLogger,
    arguments: Map<String, GAny?>,
    warnIfNotFound: Boolean,
    schemas: GInputFiles,
    fallbackSchemas: GInputFiles,
    executableDocuments: GInputFiles,
    codegenSchemaOptions: GInputFile,
    codegenOptions: GInputFile,
    irOptions: GInputFile,
    @GManuallyWired
    operationManifest: GOutputFile,
    @GManuallyWired
    outputDirectory: GOutputDirectory,
    @GManuallyWired
    dataBuildersOutputDirectory: GOutputDirectory,
) {
  val logger = logger.asLogger()
  val plugins = loadCompilerPlugins(
      arguments = arguments,
      logger = logger,
      classLoader = ApolloCompilerPlugin::class.java.classLoader,
      warnIfNotFound = warnIfNotFound,
  )
  EntryPoints.buildSources(
      plugins = plugins,
      arguments = arguments,
      logger = logger,
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
