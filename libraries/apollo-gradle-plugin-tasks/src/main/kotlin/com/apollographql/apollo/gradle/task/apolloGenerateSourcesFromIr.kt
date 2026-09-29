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
internal fun apolloGenerateSourcesFromIr(
    logger: GLogger,
    arguments: Map<String, GAny?>,
    warnIfNotFound: Boolean,
    codegenSchemas: GInputFiles,
    irOperations: GInputFile,
    downstreamUsedCoordinates: GInputFile,
    upstreamMetadata: GInputFiles,
    codegenOptions: GInputFile,
    // outputs
    @GManuallyWired
    operationManifest: GOutputFile,
    @GManuallyWired
    outputDirectory: GOutputDirectory,
    metadataOutput: GOutputFile,
) {
  val logger = logger.asLogger()
  val plugins = loadCompilerPlugins(
      arguments = arguments,
      logger = logger,
      classLoader = ApolloCompilerPlugin::class.java.classLoader,
      warnIfNotFound = warnIfNotFound,
  )
  EntryPoints.buildSourcesFromIr(
      plugins = plugins,
      arguments = arguments,
      logger = logger,
      codegenSchemas = codegenSchemas.toInputFiles(),
      irOperations = irOperations,
      downstreamUsedCoordinates = downstreamUsedCoordinates,
      upstreamMetadata = upstreamMetadata.toInputFiles(),
      codegenOptions = codegenOptions,
      operationManifest = operationManifest,
      outputDirectory = outputDirectory,
      metadataOutput = metadataOutput,
  )
}

@GTask
internal fun apolloGenerateSourcesFromIrWithFragmentUsage(
    logger: GLogger,
    arguments: Map<String, GAny?>,
    warnIfNotFound: Boolean,
    codegenSchemas: GInputFiles,
    irOperations: GInputFile,
    downstreamUsedCoordinates: GInputFile,
    downstreamUsedFragmentNames: GInputFile,
    downstreamFragmentUsageIsComplete: Boolean,
    upstreamMetadata: GInputFiles,
    codegenOptions: GInputFile,
    irOptions: GInputFile,
    // outputs
    @GManuallyWired
    operationManifest: GOutputFile,
    @GManuallyWired
    outputDirectory: GOutputDirectory,
    metadataOutput: GOutputFile,
) {
  val logger = logger.asLogger()
  val plugins = loadCompilerPlugins(
      arguments = arguments,
      logger = logger,
      classLoader = ApolloCompilerPlugin::class.java.classLoader,
      warnIfNotFound = warnIfNotFound,
  )
  EntryPoints.buildSourcesFromIrWithFragmentUsage(
      plugins = plugins,
      arguments = arguments,
      logger = logger,
      codegenSchemas = codegenSchemas.toInputFiles(),
      irOperations = irOperations,
      downstreamUsedCoordinates = downstreamUsedCoordinates,
      downstreamUsedFragmentNames = downstreamUsedFragmentNames,
      downstreamFragmentUsageIsComplete = downstreamFragmentUsageIsComplete,
      upstreamMetadata = upstreamMetadata.toInputFiles(),
      codegenOptions = codegenOptions,
      irOptions = irOptions,
      operationManifest = operationManifest,
      outputDirectory = outputDirectory,
      metadataOutput = metadataOutput,
  )
}
