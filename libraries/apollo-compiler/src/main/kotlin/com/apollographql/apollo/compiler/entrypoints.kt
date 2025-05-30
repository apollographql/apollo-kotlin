package com.apollographql.apollo.compiler

import com.apollographql.apollo.annotations.ApolloInternal
import com.apollographql.apollo.compiler.ApolloCompiler.buildIrOperations
import com.apollographql.apollo.compiler.ApolloCompiler.buildSchemaAndOperationsSourcesFromIr
import com.apollographql.apollo.compiler.codegen.writeTo
import com.apollographql.apollo.compiler.internal.DefaultApolloCompilerRegistry
import com.apollographql.apollo.compiler.internal.GradleCompilerPluginLogger
import java.io.File
import java.util.ServiceLoader
import java.util.function.Consumer

/**
 * EntryPoints contains code called using reflection from the Gradle plugin.
 * This is so that the classloader can be isolated, and we can use our preferred version of
 * Kotlin and other dependencies without risking conflicts.
 *
 * It is a version of [ApolloCompiler] that takes plain [File]s and other classes available to the bootstrap classloader only.
 */
@Suppress("UNUSED") // Used from reflection
@ApolloInternal
class EntryPoints {
  fun buildCodegenSchema(
      arguments: Map<String, Any?>,
      logLevel: Int,
      warnIfNotFound: Boolean,
      normalizedSchemaFiles: List<Any>,
      warning: Consumer<String>,
      codegenSchemaOptionsFile: File,
      codegenSchemaFile: File,
  ) {
    val registry = apolloCompilerRegistry(
        arguments,
        logLevel,
        warnIfNotFound,
    )

    ApolloCompiler.buildCodegenSchema(
        schemaFiles = normalizedSchemaFiles.toInputFiles(),
        logger = warning.toLogger(),
        codegenSchemaOptions = codegenSchemaOptionsFile.toCodegenSchemaOptions(),
        foreignSchemas = registry.foreignSchemas(),
        schemaTransform = registry.schemaDocumentTransform()
    ).writeTo(codegenSchemaFile)
  }

  fun buildIr(
      arguments: Map<String, Any?>,
      logLevel: Int,
      graphqlFiles: List<Any>,
      codegenSchemaFiles: List<Any>,
      upstreamIrOperations: List<Any>,
      irOptionsFile: File,
      warning: Consumer<String>,
      irOperationsFile: File,
  ) {
    val registry = apolloCompilerRegistry(arguments, logLevel, false)

    val upstream = upstreamIrOperations.toInputFiles().map { it.file.toIrOperations() }
    ApolloCompiler.buildIrOperations(
        executableFiles = graphqlFiles.toInputFiles(),
        codegenSchema = codegenSchemaFiles.toInputFiles().map { it.file }.findCodegenSchemaFile().toCodegenSchema(),
        upstreamCodegenModels = upstream.map { it.codegenModels },
        upstreamFragmentDefinitions = upstream.flatMap { it.fragmentDefinitions },
        documentTransform = registry.executableDocumentTransform(),
        options = irOptionsFile.toIrOptions(),
        logger = warning.toLogger(),
    ).writeTo(irOperationsFile)
  }

  fun buildSourcesFromIr(
      arguments: Map<String, Any?>,
      logLevel: Int,
      warnIfNotFound: Boolean,
      codegenSchemaFiles: List<Any>,
      upstreamMetadata: List<Any>,
      irOperations: File,
      downstreamUsedCoordinates: Map<String, Map<String, Set<String>>>,
      codegenOptions: File,
      operationManifestFile: File?,
      outputDir: File,
      metadataOutputFile: File?
  ) {
    val registry = apolloCompilerRegistry(
        arguments,
        logLevel,
        warnIfNotFound
    )
    val codegenSchemaFile = codegenSchemaFiles.toInputFiles().map { it.file }.findCodegenSchemaFile()

    val codegenSchema = codegenSchemaFile.toCodegenSchema()

    val upstreamCodegenMetadata = upstreamMetadata.toInputFiles().map { it.file.toCodegenMetadata() }
    ApolloCompiler.buildSchemaAndOperationsSourcesFromIr(
        codegenSchema = codegenSchema,
        irOperations = irOperations.toIrOperations(),
        downstreamUsedCoordinates = downstreamUsedCoordinates.toUsedCoordinates(),
        upstreamCodegenMetadata = upstreamCodegenMetadata,
        codegenOptions = codegenOptions.toCodegenOptions(),
        layout = registry.layout(codegenSchema),
        irOperationsTransform = registry.irOperationsTransform(),
        javaOutputTransform = registry.javaOutputTransform(),
        kotlinOutputTransform = registry.kotlinOutputTransform(),
        operationManifestFile = operationManifestFile,
        operationIdsGenerator = registry.toOperationIdsGenerator(),
    ).writeTo(outputDir, true, metadataOutputFile)

    if (upstreamCodegenMetadata.isEmpty()) {
      registry.schemaCodeGenerator().generate(codegenSchema.schema.toGQLDocument(), outputDir)
    }
  }

  fun buildSources(
      arguments: Map<String, Any?>,
      logLevel: Int,
      warnIfNotFound: Boolean,
      schemaFiles: List<Any>,
      graphqlFiles: List<Any>,
      codegenSchemaOptions: File,
      codegenOptions: File,
      irOptions: File,
      warning: Consumer<String>,
      operationManifestFile: File?,
      outputDir: File,
      dataBuildersOutputDir: File,
  ) {
    val registry = apolloCompilerRegistry(
        arguments,
        logLevel,
        warnIfNotFound
    )

    val codegenSchema = ApolloCompiler.buildCodegenSchema(
        schemaFiles = schemaFiles.toInputFiles(),
        codegenSchemaOptions = codegenSchemaOptions.toCodegenSchemaOptions(),
        foreignSchemas = registry.foreignSchemas(),
        logger = warning.toLogger(),
        schemaTransform = registry.schemaDocumentTransform()
    )

    val irOperations = buildIrOperations(
        codegenSchema = codegenSchema,
        executableFiles = graphqlFiles.toInputFiles(),
        upstreamCodegenModels = emptyList(),
        upstreamFragmentDefinitions = emptyList(),
        documentTransform = registry.executableDocumentTransform(),
        options = irOptions.toIrOptions(),
        logger = warning.toLogger(),
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
        operationManifestFile = operationManifestFile,
        operationIdsGenerator = registry.toOperationIdsGenerator(),
    )

    if (codegenSchema.schema.generateDataBuilders) {
      ApolloCompiler.buildDataBuilders(
          codegenSchema,
          irOperations.usedCoordinates,
          codegenOptions,
          layout,
          listOf(sourceOutput.codegenMetadata)
      ).writeTo(dataBuildersOutputDir, true, null)
    }

    sourceOutput.writeTo(outputDir, true, null)

    registry.schemaCodeGenerator().generate(codegenSchema.schema.toGQLDocument(), outputDir)
  }

  fun buildDataBuilders(
      arguments: Map<String, Any?>,
      logLevel: Int,
      warnIfNotFound: Boolean,
      codegenSchemaFiles: List<Any>,
      upstreamMetadata: List<Any>,
      downstreamUsedCoordinates: Map<String, Map<String, Set<String>>>,
      codegenOptions: File,
      outputDir: File
  ) {
    val registry = apolloCompilerRegistry(
        arguments,
        logLevel,
        warnIfNotFound,
    )
    val codegenSchemaFile = codegenSchemaFiles.toInputFiles().map { it.file }.findCodegenSchemaFile()
    val codegenSchema = codegenSchemaFile.toCodegenSchema()
    val upstreamCodegenMetadata = upstreamMetadata.toInputFiles().map { it.file.toCodegenMetadata() }

    ApolloCompiler.buildDataBuilders(
        codegenSchema = codegenSchema,
        usedCoordinates = downstreamUsedCoordinates.toUsedCoordinates(),
        codegenOptions = codegenOptions.toCodegenOptions(),
        layout = registry.layout(codegenSchema),
        upstreamCodegenMetadata = upstreamCodegenMetadata,
    ).writeTo(outputDir, true, null)
  }
}

internal fun Consumer<String>.toLogger(): ApolloCompiler.Logger {
  return object : ApolloCompiler.Logger {
    override fun warning(message: String) {
      accept(message)
    }
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
    logLevel: Int,
    warnIfNotFound: Boolean = false
): DefaultApolloCompilerRegistry {
  val registry = DefaultApolloCompilerRegistry()
  val environment = ApolloCompilerPluginEnvironment(
      arguments,
      GradleCompilerPluginLogger(logLevel),
  )
  var hasPlugin = false
  val plugins = ServiceLoader.load(ApolloCompilerPlugin::class.java, ApolloCompilerPlugin::class.java.classLoader).toList()
  plugins.forEach {
    hasPlugin = true
    it.beforeCompilationStep(environment, registry)
    registry.registerPlugin(it)
  }

  @Suppress("DEPRECATION")
  val pluginProviders = ServiceLoader.load(ApolloCompilerPluginProvider::class.java, ApolloCompilerPluginProvider::class.java.classLoader).toList()
  pluginProviders.forEach {
    // we make an exception for our own cache plugin because we want to display a nice error message to users before 4.3
    if (it.javaClass.name != "com.apollographql.cache.apollocompilerplugin.ApolloCacheCompilerPluginProvider") {
      println("Apollo: using ApolloCompilerPluginProvider is deprecated. Please use ApolloCompilerPlugin directly.")
    }
    hasPlugin = true
    val plugin = it.create(environment)
    plugin.beforeCompilationStep(environment, registry)
    registry.registerPlugin(plugin)
  }

  if (!hasPlugin && warnIfNotFound) {
    println("Apollo: a compiler plugin was added with `Service.plugin()` but no plugin was loaded by the ServiceLoader. Check your META-INF/services/com.apollographql.apollo.compiler.ApolloCompilerPlugin file.")
  }

  return registry
}

internal fun List<Any>.toInputFiles(): List<InputFile> = buildList {
  val iterator = this@toInputFiles.iterator()
  while (iterator.hasNext()) {
    add(InputFile(normalizedPath = iterator.next() as String, file = iterator.next() as File))
  }
}