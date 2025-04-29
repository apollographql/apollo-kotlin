package com.apollographql.apollo.compiler

import com.apollographql.apollo.annotations.ApolloInternal
import com.apollographql.apollo.compiler.ApolloCompiler.buildIrOperations
import com.apollographql.apollo.compiler.ApolloCompiler.buildSchemaAndOperationsSourcesFromIr
import com.apollographql.apollo.compiler.codegen.writeTo
import com.apollographql.apollo.compiler.internal.GradleCompilerPluginLogger
import com.apollographql.apollo.compiler.operationoutput.OperationDescriptor
import com.apollographql.apollo.compiler.operationoutput.OperationId
import java.io.File
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
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
    val plugin = apolloCompilerPlugin(
        arguments,
        logLevel,
        warnIfNotFound
    )

    ApolloCompiler.buildCodegenSchema(
        schemaFiles = normalizedSchemaFiles.toInputFiles(),
        logger = warning.toLogger(),
        codegenSchemaOptions = codegenSchemaOptionsFile.toCodegenSchemaOptions(),
        foreignSchemas = plugin?.foreignSchemas().orEmpty(),
        schemaTransform = plugin?.schemaTransform()
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
    buildIrOperations(
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
    val plugin = apolloCompilerPlugin(
        arguments,
        logLevel,
        warnIfNotFound
    )
    val codegenSchemaFile = codegenSchemaFiles.toInputFiles().map { it.file }.findCodegenSchemaFile()
    val codegenSchema = codegenSchemaFile.toCodegenSchema()

    val upstreamCodegenMetadata = upstreamMetadata.toInputFiles().map { it.file.toCodegenMetadata() }
    buildSchemaAndOperationsSourcesFromIr(
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
        operationIdsGenerator = plugin?.toOperationIdsGenerator(),
    ).writeTo(outputDir, true, metadataOutputFile)

    if (upstreamCodegenMetadata.isEmpty()) {
      plugin?.schemaListener()?.onSchema(codegenSchema.schema, outputDir)
    }
  }

  fun buildSources(
      arguments: Map<String, Any?>,
      logLevel: Int,
      warnIfNotFound: Boolean,
      schemaFiles: List<Any>,
      graphqlFiles: List<Any>,
      codegenSchemaOptionsFile: File,
      codegenOptionsFile: File,
      irOptions: File,
      warning: Consumer<String>,
      operationManifestFile: File?,
      outputDir: File,
      dataBuildersOutputDir: File,
  ) {
    val plugin = apolloCompilerPlugin(
        arguments,
        logLevel,
        warnIfNotFound
    )

    val codegenSchemaOptions = codegenSchemaOptionsFile.toCodegenSchemaOptions()
    val codegenSchema = ApolloCompiler.buildCodegenSchema(
        schemaFiles = schemaFiles.toInputFiles(),
        codegenSchemaOptions = codegenSchemaOptions,
        foreignSchemas = plugin?.foreignSchemas().orEmpty(),
        logger = warning.toLogger(),
        schemaTransform = plugin?.schemaTransform()
    )

    val irOperations = buildIrOperations(
        codegenSchema = codegenSchema,
        executableFiles = graphqlFiles.toInputFiles(),
        upstreamCodegenModels = emptyList(),
        upstreamFragmentDefinitions = emptyList(),
        documentTransform = plugin?.documentTransform(),
        options = irOptions.toIrOptions(),
        logger = warning.toLogger(),
    )

    val codegenOptions = codegenOptionsFile.toCodegenOptions()
    val layout = plugin?.layout(codegenSchema)
    val sourceOutput = buildSchemaAndOperationsSourcesFromIr(
        codegenSchema = codegenSchema,
        irOperations = irOperations,
        downstreamUsedCoordinates = UsedCoordinates(),
        upstreamCodegenMetadata = emptyList(),
        codegenOptions = codegenOptions,
        layout = layout,
        irOperationsTransform = plugin?.irOperationsTransform(),
        javaOutputTransform = plugin?.javaOutputTransform(),
        kotlinOutputTransform = plugin?.kotlinOutputTransform(),
        operationManifestFile = operationManifestFile,
        operationIdsGenerator = plugin?.toOperationIdsGenerator(),
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

    plugin?.schemaListener()?.onSchema(codegenSchema.schema, outputDir)
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
    val plugin = apolloCompilerPlugin(
        arguments,
        logLevel,
        warnIfNotFound
    )
    val codegenSchemaFile = codegenSchemaFiles.toInputFiles().map { it.file }.findCodegenSchemaFile()
    val codegenSchema = codegenSchemaFile.toCodegenSchema()
    val upstreamCodegenMetadata = upstreamMetadata.toInputFiles().map { it.file.toCodegenMetadata() }

    ApolloCompiler.buildDataBuilders(
        codegenSchema = codegenSchema,
        usedCoordinates = downstreamUsedCoordinates.toUsedCoordinates(),
        codegenOptions = codegenOptions.toCodegenOptions(),
        layout = plugin?.layout(codegenSchema),
        upstreamCodegenMetadata = upstreamCodegenMetadata,
    ).writeTo(outputDir, true, null)
  }
}

internal fun ApolloCompilerPlugin.toOperationIdsGenerator(): OperationIdsGenerator {

  return object : OperationIdsGenerator {
    private fun String.sha256(): String {
      val bytes = toByteArray(charset = StandardCharsets.UTF_8)
      val md = MessageDigest.getInstance("SHA-256")
      val digest = md.digest(bytes)
      return digest.fold("") { str, it -> str + "%02x".format(it) }
    }

    override fun generate(operationDescriptorList: Collection<OperationDescriptor>): List<OperationId> {
      val operationIds = operationIds(operationDescriptorList.toList())
      return if (operationIds == null) {
        operationDescriptorList.map {
          OperationId(it.source.sha256(), it.name)
        }
      } else {
        operationIds
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
    /*
     * TODO v5: simplify this and add a schema { } block to the Gradle configuration
     */
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