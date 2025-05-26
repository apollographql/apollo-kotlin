package com.apollographql.apollo.compiler

import com.apollographql.apollo.annotations.ApolloInternal
import com.apollographql.apollo.compiler.ApolloCompiler.buildIrOperations
import com.apollographql.apollo.compiler.ApolloCompiler.buildSchemaAndOperationsSourcesFromIr
import com.apollographql.apollo.compiler.codegen.writeTo
import com.apollographql.apollo.compiler.internal.DefaultApolloCompilerRegistry
import java.io.File
import java.util.ServiceLoader

/**
 * EntryPoints is a higher level API compared to [ApolloCompiler].
 * It deals with compiler plugins and serializing/deserializing files.
 */
@ApolloInternal
object EntryPoints {
  fun buildCodegenSchema(
      arguments: Map<String, Any?>,
      logger: ApolloCompiler.Logger,
      warnIfNotFound: Boolean,
      normalizedSchemaFiles: List<InputFile>,
      codegenSchemaOptionsFile: File,
      codegenSchemaFile: File,
  ) {
    val registry = apolloCompilerRegistry(
        arguments = arguments,
        logger = logger,
        warnIfNotFound = warnIfNotFound,
        outputDirectory = null
    )

    ApolloCompiler.buildCodegenSchema(
        schemaFiles = normalizedSchemaFiles,
        logger = logger,
        codegenSchemaOptions = codegenSchemaOptionsFile.toCodegenSchemaOptions(),
        foreignSchemas = registry.foreignSchemas(),
        schemaTransform = registry.schemaTransform()
    ).writeTo(codegenSchemaFile)
  }

  fun buildIr(
      arguments: Map<String, Any?>,
      logger: ApolloCompiler.Logger,
      warnIfNotFound: Boolean,
      graphqlFiles: List<InputFile>,
      codegenSchemaFiles: List<InputFile>,
      upstreamIrOperations: List<InputFile>,
      irOptionsFile: File,
      irOperationsFile: File,
  ) {
    val registry = apolloCompilerRegistry(arguments, logger, warnIfNotFound, outputDirectory = null)

    val upstream = upstreamIrOperations.map { it.file.toIrOperations() }
    buildIrOperations(
        executableFiles = graphqlFiles,
        codegenSchema = codegenSchemaFiles.map { it.file }.findCodegenSchemaFile().toCodegenSchema(),
        upstreamCodegenModels = upstream.map { it.codegenModels },
        upstreamFragmentDefinitions = upstream.flatMap { it.fragmentDefinitions },
        operationsTransform = registry.operationsTransform(),
        options = irOptionsFile.toIrOptions(),
        logger = logger,
    ).writeTo(irOperationsFile)
  }

  fun buildSourcesFromIr(
      arguments: Map<String, Any?>,
      logger: ApolloCompiler.Logger,
      warnIfNotFound: Boolean,
      codegenSchemas: List<InputFile>,
      upstreamMetadata: List<InputFile>,
      irOperations: File,
      usedCoordinates: File,
      codegenOptions: File,
      operationManifest: File?,
      outputDirectory: File,
      metadataOutput: File?
  ) {
    val registry = apolloCompilerRegistry(
        arguments,
        logger,
        warnIfNotFound,
        outputDirectory
    )
    val codegenSchemaFile = codegenSchemas.map { it.file }.findCodegenSchemaFile()
    val codegenSchema = codegenSchemaFile.toCodegenSchema()

    val upstreamCodegenMetadata = upstreamMetadata.map { it.file.toCodegenMetadata() }
    buildSchemaAndOperationsSourcesFromIr(
        codegenSchema = codegenSchema,
        irOperations = irOperations.toIrOperations(),
        downStreamUsedCoordinates = usedCoordinates.toUsedCoordinates(),
        upstreamCodegenMetadata = upstreamCodegenMetadata,
        codegenOptions = codegenOptions.toCodegenOptions(),
        layout = registry.layout(codegenSchema),
        irOperationsTransform = registry.irOperationsTransform(),
        javaOutputTransform = registry.javaOutputTransform(),
        kotlinOutputTransform = registry.kotlinOutputTransform(),
        operationManifestFile = operationManifest,
        operationIdsGenerator = registry.toOperationIdsGenerator(),
    ).writeTo(outputDirectory, true, metadataOutput)

    if (upstreamCodegenMetadata.isEmpty()) {
      registry.extraCodeGenerator().generate(codegenSchema.schema.toGQLDocument())
    }
  }

  fun buildSources(
      arguments: Map<String, Any?>,
      logger: ApolloCompiler.Logger,
      warnIfNotFound: Boolean,
      schemas: List<InputFile>,
      executableDocuments: List<InputFile>,
      codegenSchemaOptions: File,
      codegenOptions: File,
      irOptions: File,
      operationManifest: File?,
      outputDirectory: File,
      dataBuildersOutputDirectory: File,
  ) {
    val registry = apolloCompilerRegistry(
        arguments,
        logger,
        warnIfNotFound,
        outputDirectory
    )

    @Suppress("NAME_SHADOWING")
    val codegenSchemaOptions = codegenSchemaOptions.toCodegenSchemaOptions()
    val codegenSchema = ApolloCompiler.buildCodegenSchema(
        schemaFiles = schemas,
        codegenSchemaOptions = codegenSchemaOptions,
        foreignSchemas = registry.foreignSchemas(),
        logger = logger,
        schemaTransform = registry.schemaTransform()
    )

    val irOperations = buildIrOperations(
        codegenSchema = codegenSchema,
        executableFiles = executableDocuments,
        upstreamCodegenModels = emptyList(),
        upstreamFragmentDefinitions = emptyList(),
        operationsTransform = registry.operationsTransform(),
        options = irOptions.toIrOptions(),
        logger = logger,
    )

    @Suppress("NAME_SHADOWING")
    val codegenOptions = codegenOptions.toCodegenOptions()
    val layout = registry.layout(codegenSchema)
    val sourceOutput = buildSchemaAndOperationsSourcesFromIr(
        codegenSchema = codegenSchema,
        irOperations = irOperations,
        downStreamUsedCoordinates = UsedCoordinates(),
        upstreamCodegenMetadata = emptyList(),
        codegenOptions = codegenOptions,
        layout = layout,
        irOperationsTransform = registry.irOperationsTransform(),
        javaOutputTransform = registry.javaOutputTransform(),
        kotlinOutputTransform = registry.kotlinOutputTransform(),
        operationManifestFile = operationManifest,
        operationIdsGenerator = registry.toOperationIdsGenerator(),
    )

    if (codegenSchema.schema.generateDataBuilders) {
      ApolloCompiler.buildDataBuilders(
          codegenSchema,
          irOperations.usedCoordinates,
          codegenOptions,
          layout,
          listOf(sourceOutput.codegenMetadata)
      ).writeTo(dataBuildersOutputDirectory, true, null)
    }

    sourceOutput.writeTo(outputDirectory, true, null)

    registry.extraCodeGenerator().generate(codegenSchema.schema.toGQLDocument())
  }

  fun buildDataBuilders(
      arguments: Map<String, Any?>,
      logger: ApolloCompiler.Logger,
      warnIfNotFound: Boolean,
      codegenSchemas: List<InputFile>,
      upstreamMetadatas: List<InputFile>,
      downstreamUsedCoordinates: File,
      codegenOptions: File,
      outputDirectory: File
  ) {
    val registry = apolloCompilerRegistry(
        arguments,
        logger,
        warnIfNotFound,
        outputDirectory
    )
    val codegenSchemaFile = codegenSchemas.map { it.file }.findCodegenSchemaFile()
    val codegenSchema = codegenSchemaFile.toCodegenSchema()
    val upstreamCodegenMetadata = upstreamMetadatas.map { it.file.toCodegenMetadata() }

    ApolloCompiler.buildDataBuilders(
        codegenSchema = codegenSchema,
        usedCoordinates = downstreamUsedCoordinates.toUsedCoordinates(),
        codegenOptions = codegenOptions.toCodegenOptions(),
        layout = registry.layout(codegenSchema),
        upstreamCodegenMetadata = upstreamCodegenMetadata,
    ).writeTo(outputDirectory, true, null)
  }
}

@ApolloInternal
fun Iterable<File>.findCodegenSchemaFile(): File {
  return firstOrNull {
    /*
     * TODO v5: simplify this and add a schema { } block to the Gradle configuration
     */
    it.length() > 0
  } ?: error("Cannot find CodegenSchema in $this")
}

internal fun apolloCompilerRegistry(
    arguments: Map<String, Any?>,
    logger: ApolloCompiler.Logger,
    warnIfNotFound: Boolean = false,
    outputDirectory: File?
): DefaultApolloCompilerRegistry {
  val registry = DefaultApolloCompilerRegistry()
  val environment = ApolloCompilerPluginEnvironment(
      arguments,
      logger,
      outputDirectory
  )
  var hasPlugin = false
  val plugins = ServiceLoader.load(ApolloCompilerPlugin::class.java, ApolloCompilerPlugin::class.java.classLoader).toList()
  plugins.forEach {
    hasPlugin = true
    it.beforeCompilationStep(environment, registry)
  }

  val pluginProviders = ServiceLoader.load(ApolloCompilerPluginProvider::class.java, ApolloCompilerPluginProvider::class.java.classLoader).toList()
  pluginProviders.forEach {
    if (hasPlugin)  {
      error("Apollo: exposing both an ApolloCompilerPluginProvider and an ApolloCompilerPlugin is deprecated. Please use ApolloCompilerPlugin directly.")
    }
    println("Apollo: using ApolloCompilerPluginProvider is deprecated. Please use ApolloCompilerPlugin directly.")
    hasPlugin = true
    val plugin = it.create(environment)
    plugin.beforeCompilationStep(environment, registry)
    registry.registerOperationIdsGenerator(LegacyOperationIdsGenerator(plugin))
  }

  if (!hasPlugin && warnIfNotFound) {
    println("Apollo: a compiler plugin was added with `Service.plugin()` but no plugin was loaded by the ServiceLoader. Check your META-INF/services/com.apollographql.apollo.compiler.ApolloCompilerPlugin file.")
  }

  return registry
}