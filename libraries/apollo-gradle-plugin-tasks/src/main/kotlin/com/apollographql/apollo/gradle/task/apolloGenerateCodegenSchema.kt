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

  val pluginEnvironment = ApolloCompilerPluginEnvironment(
      logger = logger.asLogger(),
      arguments = arguments,
  )
  val plugins = loadCompilerPlugins(
      pluginEnvironment = pluginEnvironment,
      classLoader = ApolloCompilerPlugin::class.java.classLoader,
      warnIfNotFound = warnIfNotFound,
  )
  EntryPoints.buildCodegenSchema(
      pluginEnvironment = pluginEnvironment,
      plugins = plugins,
      normalizedSchemaFiles = (schemaFiles.takeIf { it.isNotEmpty() } ?: fallbackSchemaFiles).toInputFiles(),
      codegenSchemaOptionsFile = codegenSchemaOptionsFile,
      codegenSchemaFile = codegenSchemaFile,
  )
}
