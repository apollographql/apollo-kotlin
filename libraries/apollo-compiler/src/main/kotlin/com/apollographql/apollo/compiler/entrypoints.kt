package com.apollographql.apollo.compiler

import com.apollographql.apollo.annotations.ApolloInternal
import com.apollographql.apollo.compiler.codegen.SchemaAndOperationsLayout
import com.apollographql.apollo.compiler.codegen.writeTo
import com.apollographql.apollo.compiler.internal.GradleCompilerPluginLogger
import com.apollographql.apollo.compiler.operationoutput.OperationDescriptor
import com.apollographql.apollo.compiler.operationoutput.OperationId
import com.apollographql.apollo.compiler.operationoutput.OperationOutput
import java.io.File
import java.util.ServiceLoader
import java.util.function.Consumer

/**
 * EntryPoints contains code that is called using reflection from the Gradle plugin.
 * This is so that the classloader can be isolated, and we can use our preferred version of
 * Kotlin and other dependencies without risking conflicts.
 */
@Suppress("UNUSED") // Used from reflection
@ApolloInternal
class EntryPoints {
  fun buildCodegenSchema(
      arguments: Map<String, Any?>,
      logLevel: Int,
      warnIfNotFound: Boolean = false,
      normalizedSchemaFiles: List<Any>,
      warning: Consumer<String>,
      codegenSchemaOptionsFile: File,
      codegenSchemaFile: File,
  ) {
    val plugin = apolloCompilerPlugin(
        arguments,
        logLevel,
        warnIfNotFound
    )

    ApolloCompiler.buildCodegenSchema(
        schemaFiles = normalizedSchemaFiles.toInputFiles(),
        logger = warning.toLogger(),
        codegenSchemaOptions = codegenSchemaOptionsFile.toCodegenSchemaOptions(),
        foreignSchemas = plugin?.foreignSchemas().orEmpty()
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
    val plugin = apolloCompilerPlugin(arguments, logLevel)

    val upstream = upstreamIrOperations.toInputFiles().map { it.file.toIrOperations() }
    ApolloCompiler.buildIrOperations(
        executableFiles = graphqlFiles.toInputFiles(),
        codegenSchema = codegenSchemaFiles.toInputFiles().map { it.file }.findCodegenSchemaFile().toCodegenSchema(),
        upstreamCodegenModels = upstream.map { it.codegenModels },
        upstreamFragmentDefinitions = upstream.flatMap { it.fragmentDefinitions },
        documentTransform = plugin?.documentTransform(),
        options = irOptionsFile.toIrOptions(),
        logger = warning.toLogger(),
    ).writeTo(irOperationsFile)
  }

  fun buildSourcesFromIr(
      arguments: Map<String, Any?>,
      logLevel: Int,
      warnIfNotFound: Boolean = false,
      codegenSchemaFiles: List<Any>,
      upstreamMetadata: List<Any>,
      irOperations: File,
      downstreamUsedCoordinates: Map<String, Map<String, Set<String>>>,
      codegenOptions: File,
      operationManifestFile: File?,
      outputDir: File,
      metadataOutputFile: File?
  ) {
    val plugin = apolloCompilerPlugin(
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
        layout = plugin?.layout(codegenSchema),
        irOperationsTransform = plugin?.irOperationsTransform(),
        javaOutputTransform = plugin?.javaOutputTransform(),
        kotlinOutputTransform = plugin?.kotlinOutputTransform(),
        operationManifestFile = operationManifestFile,
        operationOutputGenerator = plugin?.toOperationOutputGenerator(),
    ).writeTo(outputDir, true, metadataOutputFile)

    if (upstreamCodegenMetadata.isEmpty()) {
      plugin?.schemaListener()?.onSchema(codegenSchema.schema, outputDir)
    }
  }

  fun buildSources(
      arguments: Map<String, Any?>,
      logLevel: Int,
      warnIfNotFound: Boolean = false,
      schemaFiles: List<Any>,
      graphqlFiles: List<Any>,
      codegenSchemaOptions: File,
      codegenOptions: File,
      irOptions: File,
      warning: Consumer<String>,
      operationManifestFile: File?,
      outputDir: File
  ) {
    val plugin = apolloCompilerPlugin(
        arguments,
        logLevel,
        warnIfNotFound
    )

    val codegenSchema = ApolloCompiler.buildCodegenSchema(
        schemaFiles = schemaFiles.toInputFiles(),
        codegenSchemaOptions = codegenSchemaOptions.toCodegenSchemaOptions(),
        foreignSchemas = plugin?.foreignSchemas().orEmpty(),
        logger = warning.toLogger()
    )

    ApolloCompiler.buildSchemaAndOperationsSources(
        codegenSchema,
        executableFiles = graphqlFiles.toInputFiles(),
        codegenOptions = codegenOptions.toCodegenOptions(),
        irOptions = irOptions.toIrOptions(),
        logger = warning.toLogger(),
        layoutFactory = object : LayoutFactory {
          override fun create(codegenSchema: CodegenSchema): SchemaAndOperationsLayout? {
            return plugin?.layout(codegenSchema)
          }
        },
        operationOutputGenerator = plugin?.toOperationOutputGenerator(),
        irOperationsTransform = plugin?.irOperationsTransform(),
        javaOutputTransform = plugin?.javaOutputTransform(),
        kotlinOutputTransform = plugin?.kotlinOutputTransform(),
        documentTransform = plugin?.documentTransform(),
        operationManifestFile = operationManifestFile,
    ).writeTo(outputDir, true, null)

    plugin?.schemaListener()?.onSchema(codegenSchema.schema, outputDir)
  }
}

@Suppress("DEPRECATION")
internal fun ApolloCompilerPlugin.toOperationOutputGenerator(): OperationOutputGenerator {
  return object : OperationOutputGenerator {
    override fun generate(operationDescriptorList: Collection<OperationDescriptor>): OperationOutput {
      var operationIds = operationIds(operationDescriptorList.toList())
      if (operationIds == null) {
        operationIds = operationDescriptorList.map { OperationId(OperationIdGenerator.Sha256.apply(it.source, it.name), it.name) }
      }
      return operationDescriptorList.associateBy { descriptor ->
        val operationId = operationIds.firstOrNull { it.name == descriptor.name } ?: error("No id found for operation ${descriptor.name}")
        operationId.id
      }
    }
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
    it.length() > 0
  } ?: error("Cannot find CodegenSchema in $this")
}

internal fun apolloCompilerPlugin(
    arguments: Map<String, Any?>,
    logLevel: Int,
    warnIfNotFound: Boolean = false,
): ApolloCompilerPlugin? {
  val plugins = ServiceLoader.load(ApolloCompilerPlugin::class.java, ApolloCompilerPlugin::class.java.classLoader).toList()

  if (plugins.size > 1) {
    error("Apollo: only a single compiler plugin is allowed")
  }

  val plugin = plugins.singleOrNull()
  if (plugin != null) {
    error("Apollo: use ApolloCompilerPluginProvider instead of ApolloCompilerPlugin directly. ApolloCompilerPluginProvider allows arguments and logging")
  }

  val pluginProviders = ServiceLoader.load(ApolloCompilerPluginProvider::class.java, ApolloCompilerPlugin::class.java.classLoader).toList()

  if (pluginProviders.size > 1) {
    error("Apollo: only a single compiler plugin provider is allowed")
  }

  if (pluginProviders.isEmpty() && warnIfNotFound) {
    println("Apollo: a compiler plugin was added with `Service.plugin()` but could not be loaded by the ServiceLoader. Check your META-INF/services/com.apollographql.apollo.compiler.ApolloCompilerPluginProvider file.")
  }

  val provider = pluginProviders.singleOrNull()
  if (provider != null) {
    return provider.create(
        ApolloCompilerPluginEnvironment(
            arguments,
            GradleCompilerPluginLogger(logLevel)
        )
    )
  }

  return plugins.singleOrNull()
}


internal fun List<Any>.toInputFiles(): List<InputFile> = buildList {
  val iterator = this@toInputFiles.iterator()
  while (iterator.hasNext()) {
    add(InputFile(normalizedPath = iterator.next() as String, file = iterator.next() as File))
  }
}