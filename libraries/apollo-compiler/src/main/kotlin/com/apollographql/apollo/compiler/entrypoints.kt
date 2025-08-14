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
      arguments: Map<String, Any?>,
      logger: ApolloCompiler.Logger,
      warnIfNotFound: Boolean,
      graphqlFiles: List<InputFile>,
      codegenSchemaFiles: List<InputFile>,
      upstreamIrOperations: List<InputFile>,
      irOptionsFile: File,
      irOperationsFile: File,
  ) {
    val registry = apolloCompilerRegistry(arguments, logger, warnIfNotFound)

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
    )
    val codegenSchemaFile = codegenSchemas.map { it.file }.findCodegenSchemaFile()
    val codegenSchema = codegenSchemaFile.toCodegenSchema()

    val upstreamCodegenMetadata = upstreamMetadata.map { it.file.toCodegenMetadata() }
    buildSchemaAndOperationsSourcesFromIr(
        codegenSchema = codegenSchema,
        irOperations = irOperations.toIrOperations(),
        downstreamUsedCoordinates = usedCoordinates.toUsedCoordinates(),
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

@ApolloInternal
fun apolloCompilerRegistry(
    arguments: Map<String, Any?>,
    logger: ApolloCompiler.Logger,
    warnIfNotFound: Boolean = false,
    classLoader: ClassLoader = ApolloCompilerPlugin::class.java.classLoader,
): DefaultApolloCompilerRegistry {
  val registry = DefaultApolloCompilerRegistry()
  val environment = ApolloCompilerPluginEnvironment(
      arguments,
      logger,
  )
  var hasPlugin = false
  val plugins = ServiceLoader.load(ApolloCompilerPlugin::class.java, classLoader).toList()
  plugins.forEach {
    hasPlugin = true
    it.beforeCompilationStep(environment, registry)
    registry.registerPlugin(it)
  }

  @Suppress("DEPRECATION")
  val pluginProviders = ServiceLoader.load(ApolloCompilerPluginProvider::class.java, classLoader).toList()
  pluginProviders.forEach {
    // we make an exception for our own cache plugin because we want to display a nice error message to users before 4.3
    if (it.javaClass.name != "com.apollographql.cache.apollocompilerplugin.ApolloCacheCompilerPluginProvider") {
      println("Apollo: using ApolloCompilerPluginProvider is deprecated. You can use ApolloCompilerPlugin directly. See https://go.apollo.dev/ak-compiler-plugins for more details.")
    }
    hasPlugin = true
    val plugin = it.create(environment)
    plugin.beforeCompilationStep(environment, registry)
    registry.registerPlugin(plugin)
  }

  if (!hasPlugin && warnIfNotFound) {
    println("Apollo: a compiler plugin was added with `Service.plugin()` but no plugin was loaded by the ServiceLoader. Check your META-INF/services/com.apollographql.apollo.compiler.ApolloCompilerPlugin file. See https://go.apollo.dev/ak-compiler-plugins for more details.")
  }

  return registry
}
