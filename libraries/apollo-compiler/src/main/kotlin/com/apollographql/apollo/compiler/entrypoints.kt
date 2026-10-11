package com.apollographql.apollo.compiler

import com.apollographql.apollo.annotations.ApolloInternal
import com.apollographql.apollo.ast.GQLFragmentDefinition
import com.apollographql.apollo.ast.GQLFragmentSpread
import com.apollographql.apollo.ast.GQLNode
import com.apollographql.apollo.ast.ParserOptions
import com.apollographql.apollo.ast.UnusedFragment
import com.apollographql.apollo.ast.checkEmpty
import com.apollographql.apollo.ast.parseAsGQLDocument
import com.apollographql.apollo.ast.pretty
import com.apollographql.apollo.ast.toGQLDocument
import com.apollographql.apollo.compiler.ApolloCompiler.buildIrOperations
import com.apollographql.apollo.compiler.ApolloCompiler.buildSchemaAndOperationsSourcesFromIr
import com.apollographql.apollo.compiler.codegen.writeTo
import com.apollographql.apollo.compiler.internal.DefaultApolloCompilerRegistry
import okio.buffer
import okio.source
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

  @JvmOverloads
  @Suppress("DEPRECATION")
  fun buildIr(
      plugins: List<ApolloCompilerPlugin>,
      arguments: Map<String, Any?>,
      logger: ApolloCompiler.Logger,
      graphqlFiles: List<InputFile>,
      codegenSchemaFiles: List<InputFile>,
      upstreamIrOperations: List<InputFile>,
      irOptionsFile: File,
      irOperationsFile: File,
      reportUnusedFragments: Boolean = true,
  ) {
    val registry = apolloCompilerRegistry(
        arguments = arguments,
        logger = logger,
        plugins = plugins,
    )

    val upstream = upstreamIrOperations.map { it.file.toIrOperations() }
    val options = irOptionsFile.toIrOptions().let {
      if (reportUnusedFragments) {
        it
      } else {
        // Downstream usage is unavailable until all participating modules have built their IR.
        buildIrOptions(
            decapitalizeFields = it.decapitalizeFields,
            flattenModels = it.flattenModels,
            failOnWarnings = it.failOnWarnings,
            addTypename = it.addTypename,
            generateOptionalOperationVariables = it.generateOptionalOperationVariables,
            alwaysGenerateTypesMatching = it.alwaysGenerateTypesMatching,
            codegenModels = it.codegenModels,
            issueSeverity = it.issueSeverities.orEmpty() + ("UnusedFragment" to IssueSeverity.Ignore),
            allowFragmentArguments = it.allowFragmentArguments,
            allowEmptySelectionSets = it.allowEmptySelectionSets,
        )
      }
    }
    buildIrOperations(
        executableFiles = graphqlFiles,
        codegenSchema = codegenSchemaFiles.map { it.file }.findCodegenSchemaFile().toCodegenSchema(),
        upstreamCodegenModels = upstream.map { it.codegenModels },
        upstreamFragmentDefinitions = upstream.flatMap { it.fragmentDefinitions },
        documentTransform = registry.executableDocumentTransform(),
        options = options,
        logger = logger,
    ).writeTo(irOperationsFile)
  }

  fun computeUnusedFragments(
      irOperations: File,
      downstreamIrOperations: List<InputFile>,
      downstreamFragmentUsageIsComplete: Boolean,
      outputFile: File,
  ) {
    if (!downstreamFragmentUsageIsComplete) {
      // Exported fragments may have consumers outside the registered dependency graph.
      outputFile.writeText("")
      return
    }

    val localIr = irOperations.toIrOperations()
    val usedNames = mutableSetOf<String>()
    fun collectSpreads(node: GQLNode) {
      if (node is GQLFragmentSpread) {
        usedNames.add(node.name)
      }
      node.children.forEach(::collectSpreads)
    }

    val parserOptions = ParserOptions.Builder().allowFragmentArguments(true).build()
    (listOf(localIr) + downstreamIrOperations.map { it.file.toIrOperations() }).forEach { ir ->
      ir.fragmentDefinitions.forEach(::collectSpreads)
      ir.operations.forEach {
        collectSpreads(it.sourceWithFragments.toGQLDocument(parserOptions))
      }
    }

    val unusedNames = localIr.fragmentDefinitions.map { it.name }.filterNot { it in usedNames }.sorted()
    // Only changes to this module's diagnostics should invalidate source generation.
    outputFile.writeText(unusedNames.joinToString("\n"))
  }

  fun checkUnusedFragments(
      unusedFragments: File,
      graphqlFiles: List<InputFile>,
      irOptionsFile: File,
      logger: ApolloCompiler.Logger,
  ) {
    val unusedNames = unusedFragments.readLines()
    if (unusedNames.isEmpty()) {
      return
    }

    val options = irOptionsFile.toIrOptions()
    val parserOptions = ParserOptions.Builder().allowFragmentArguments(true).build()
    val locations = graphqlFiles.flatMap {
      it.file.source().buffer().parseAsGQLDocument(it.file.path, parserOptions).getOrThrow().definitions
          .filterIsInstance<GQLFragmentDefinition>()
    }.associate { it.name to it.sourceLocation }
    val issues = unusedNames.map {
      UnusedFragment("Fragment '$it' is not used", locations[it])
    }.group(defaultIssueSeverities + options.issueSeverities.orEmpty())

    issues.errors.checkEmpty()
    issues.warnings.forEach {
      logger.warning("w: ${it.sourceLocation.pretty()}: Apollo: ${it.message}")
    }
    if ((options.failOnWarnings ?: defaultFailOnWarnings) && issues.warnings.isNotEmpty()) {
      error("Apollo: Warnings found and 'failOnWarnings' is true, aborting.")
    }
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
