package com.apollographql.apollo.compiler

import com.apollographql.apollo.annotations.ApolloInternal
import com.apollographql.apollo.compiler.ApolloCompiler.buildIrOperations
import com.apollographql.apollo.compiler.ApolloCompiler.buildSchemaAndOperationsSourcesFromIr
import com.apollographql.apollo.compiler.codegen.writeTo
import com.apollographql.apollo.compiler.internal.DefaultApolloCompilerRegistry
import java.io.File

/**
 * EntryPoints is a higher level API compared to [ApolloCompiler].
 * It deals with compiler plugins and serializing/deserializing files.
 */
@ApolloInternal
object EntryPoints {
  fun buildCodegenSchema(
      plugins: List<ApolloCompilerPlugin>,
      arguments: Map<String, Any?>,
      logger: ApolloCompiler.Logger,
      normalizedSchemaFiles: List<InputFile>,
      codegenSchemaOptionsFile: File,
      codegenSchemaFile: File,
  ) {
    val registry = apolloCompilerRegistry(
        arguments = arguments,
        logger = logger,
        plugins = plugins,
    )

    ApolloCompiler.buildCodegenSchema(
        schemaFiles = normalizedSchemaFiles,
        logger = logger,
        codegenSchemaOptions = codegenSchemaOptionsFile.toCodegenSchemaOptions(),
        foreignSchemas = registry.foreignSchemas(),
        schemaTransform = registry.schemaDocumentTransform()
    ).writeTo(codegenSchemaFile)
  }

  fun buildIr(
      plugins: List<ApolloCompilerPlugin>,
      arguments: Map<String, Any?>,
      logger: ApolloCompiler.Logger,
      graphqlFiles: List<InputFile>,
      codegenSchemaFiles: List<InputFile>,
      upstreamIrOperations: List<InputFile>,
      irOptionsFile: File,
      irOperationsFile: File,
  ) {
    val registry = apolloCompilerRegistry(
        arguments = arguments,
        logger = logger,
        plugins = plugins,
    )

    val upstream = upstreamIrOperations.map { it.file.toIrOperations() }
    buildIrOperations(
        executableFiles = graphqlFiles,
        codegenSchema = codegenSchemaFiles.map { it.file }.findCodegenSchemaFile().toCodegenSchema(),
        upstreamCodegenModels = upstream.map { it.codegenModels },
        upstreamFragmentDefinitions = upstream.flatMap { it.fragmentDefinitions },
        documentTransform = registry.executableDocumentTransform(),
        options = irOptionsFile.toIrOptions(),
        logger = logger,
    ).writeTo(irOperationsFile)
  }

  fun buildSourcesFromIr(
      plugins: List<ApolloCompilerPlugin>,
      arguments: Map<String, Any?>,
      logger: ApolloCompiler.Logger,
      codegenSchemas: List<InputFile>,
      upstreamMetadata: List<InputFile>,
      irOperations: File,
      downstreamUsedCoordinates: File,
      codegenOptions: File,
      operationManifest: File?,
      outputDirectory: File,
      metadataOutput: File?,
  ) {
    val registry = apolloCompilerRegistry(
        arguments = arguments,
        logger = logger,
        plugins = plugins,
    )
    val codegenSchemaFile = codegenSchemas.map { it.file }.findCodegenSchemaFile()
    val codegenSchema = codegenSchemaFile.toCodegenSchema()

    val upstreamCodegenMetadata = upstreamMetadata.map { it.file.toCodegenMetadata() }
    buildSchemaAndOperationsSourcesFromIr(
        codegenSchema = codegenSchema,
        irOperations = irOperations.toIrOperations(),
        downstreamUsedCoordinates = downstreamUsedCoordinates.toUsedCoordinates(),
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
      registry.schemaCodeGenerator().generate(codegenSchema.schema.toGQLDocument(), outputDirectory)
    }
  }

  fun buildSources(
      plugins: List<ApolloCompilerPlugin>,
      arguments: Map<String, Any?>,
      logger: ApolloCompiler.Logger,
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
        arguments = arguments,
        logger = logger,
        plugins = plugins,
    )

    @Suppress("NAME_SHADOWING")
    val codegenSchemaOptions = codegenSchemaOptions.toCodegenSchemaOptions()
    val codegenSchema = ApolloCompiler.buildCodegenSchema(
        schemaFiles = schemas,
        codegenSchemaOptions = codegenSchemaOptions,
        foreignSchemas = registry.foreignSchemas(),
        logger = logger,
        schemaTransform = registry.schemaDocumentTransform()
    )

    val irOperations = buildIrOperations(
        codegenSchema = codegenSchema,
        executableFiles = executableDocuments,
        upstreamCodegenModels = emptyList(),
        upstreamFragmentDefinitions = emptyList(),
        documentTransform = registry.executableDocumentTransform(),
        options = irOptions.toIrOptions(),
        logger = logger,
    )

    @Suppress("NAME_SHADOWING")
    val codegenOptions = codegenOptions.toCodegenOptions()
    val layout = registry.layout(codegenSchema)
    val sourceOutput = buildSchemaAndOperationsSourcesFromIr(
        codegenSchema = codegenSchema,
        irOperations = irOperations,
        downstreamUsedCoordinates = UsedCoordinates(),
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

    registry.schemaCodeGenerator().generate(codegenSchema.schema.toGQLDocument(), outputDirectory)
  }

  fun buildDataBuilders(
      plugins: List<ApolloCompilerPlugin>,
      arguments: Map<String, Any?>,
      logger: ApolloCompiler.Logger,
      codegenSchemas: List<InputFile>,
      upstreamMetadatas: List<InputFile>,
      downstreamUsedCoordinates: File,
      irOperations: File,
      codegenOptions: File,
      outputDirectory: File,
  ) {
    val registry = apolloCompilerRegistry(
        arguments = arguments,
        logger = logger,
        plugins = plugins,
    )
    val codegenSchemaFile = codegenSchemas.map { it.file }.findCodegenSchemaFile()
    val codegenSchema = codegenSchemaFile.toCodegenSchema()
    val upstreamCodegenMetadata = upstreamMetadatas.map { it.file.toCodegenMetadata() }

    ApolloCompiler.buildDataBuilders(
        codegenSchema = codegenSchema,
        usedCoordinates = downstreamUsedCoordinates.toUsedCoordinates().mergeWith(irOperations.toIrOperations().usedCoordinates),
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
    plugins: List<ApolloCompilerPlugin>,
    arguments: Map<String, Any?>,
    logger: ApolloCompiler.Logger,
): DefaultApolloCompilerRegistry {
  val registry = DefaultApolloCompilerRegistry()
  val environment = ApolloCompilerPluginEnvironment(arguments, logger)
  plugins.forEach {
    it.beforeCompilationStep(environment, registry)
    registry.registerPlugin(it)
  }
  return registry
}
