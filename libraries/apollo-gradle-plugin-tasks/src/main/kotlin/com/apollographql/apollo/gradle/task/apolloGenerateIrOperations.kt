package com.apollographql.apollo.gradle.task

import com.apollographql.apollo.compiler.ApolloCompilerPlugin
import com.apollographql.apollo.compiler.ApolloCompilerPluginEnvironment
import com.apollographql.apollo.compiler.EntryPoints
import com.apollographql.apollo.compiler.loadCompilerPlugins
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
  val pluginEnvironment = ApolloCompilerPluginEnvironment(
      logger = logger.asLogger(),
      arguments = arguments,
  )
  val plugins = loadCompilerPlugins(
      pluginEnvironment = pluginEnvironment,
      classLoader = ApolloCompilerPlugin::class.java.classLoader,
      warnIfNotFound = warnIfNotFound,
  )
  EntryPoints.buildIr(
      pluginEnvironment = pluginEnvironment,
      plugins = plugins,
      graphqlFiles = graphqlFiles.toInputFiles(),
      codegenSchemaFiles = codegenSchemas.toInputFiles(),
      upstreamIrOperations = upstreamIrFiles.toInputFiles(),
      irOptionsFile = irOptionsFile,
      irOperationsFile = irOperationsFile,
  )
}
