package com.apollographql.apollo.gradle.task

import com.apollographql.apollo.compiler.ApolloCompilerPlugin
import com.apollographql.apollo.compiler.ApolloCompilerPluginEnvironment
import com.apollographql.apollo.compiler.EntryPoints
import com.apollographql.apollo.compiler.loadCompilerPlugins
import gratatouille.tasks.GAny
import gratatouille.tasks.GInputFile
import gratatouille.tasks.GInputFiles
import gratatouille.tasks.GLogger
import gratatouille.tasks.GManuallyWired
import gratatouille.tasks.GOutputDirectory
import gratatouille.tasks.GTask

@GTask
internal fun apolloGenerateDataBuildersSources(
    logger: GLogger,
    arguments: Map<String, GAny?>,
    warnIfNotFound: Boolean,
    codegenSchemas: GInputFiles,
    downstreamUsedCoordinates: GInputFile,
    upstreamMetadata: GInputFiles,
    codegenOptions: GInputFile,
    @GManuallyWired
    outputDirectory: GOutputDirectory,
) {
  val pluginEnvironment = ApolloCompilerPluginEnvironment(
      logger = logger.asLogger(),
      arguments = arguments,
  )
  val plugins = loadCompilerPlugins(
      pluginEnvironment = pluginEnvironment,
      classLoader = ApolloCompilerPlugin::class.java.classLoader,
      warnIfNotFound = warnIfNotFound,
  )
  EntryPoints.buildDataBuilders(
      pluginEnvironment = pluginEnvironment,
      plugins = plugins,
      codegenSchemas = codegenSchemas.toInputFiles(),
      upstreamMetadatas = upstreamMetadata.toInputFiles(),
      downstreamUsedCoordinates = downstreamUsedCoordinates,
      codegenOptions = codegenOptions,
      outputDirectory = outputDirectory,
  )
}
