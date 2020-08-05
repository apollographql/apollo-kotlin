package com.apollographql.apollo.gradle.internal

import com.apollographql.apollo.api.internal.QueryDocumentMinifier
import com.apollographql.apollo.compiler.DefaultPackageNameProvider
import com.apollographql.apollo.compiler.GraphQLCompiler
import com.apollographql.apollo.compiler.NullableValueType
import com.apollographql.apollo.compiler.OperationOutputGenerator
import com.apollographql.apollo.compiler.Roots
import com.apollographql.apollo.compiler.ir.Fragment
import com.apollographql.apollo.compiler.operationoutput.OperationDescriptor
import com.apollographql.apollo.compiler.operationoutput.toJson
import com.apollographql.apollo.compiler.parser.graphql.GraphQLDocumentParser
import com.apollographql.apollo.compiler.parser.introspection.IntrospectionSchema
import com.apollographql.apollo.compiler.parser.introspection.IntrospectionSchema.Companion.toJson
import com.apollographql.apollo.compiler.parser.sdl.GraphSdlSchema
import com.apollographql.apollo.compiler.parser.sdl.toIntrospectionSchema
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import okio.buffer
import okio.sink
import okio.source
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.util.zip.ZipFile

@CacheableTask
abstract class ApolloGenerateSourcesTask : DefaultTask() {
  @get:OutputFile
  @get:Optional
  abstract val operationOutputFile: RegularFileProperty

  @get:OutputDirectory
  @get:Optional
  abstract val metadataOutputDir: DirectoryProperty

  @get:InputFiles
  @get:SkipWhenEmpty
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val graphqlFiles: ConfigurableFileCollection

  @get:InputFile
  @get:Optional
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val schemaFile: RegularFileProperty

  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  lateinit var metadataConfiguration: Configuration

  @get:Input
  abstract val rootFolders: ListProperty<String>

  @get:Input
  @get:Optional
  abstract val rootPackageName: Property<String>

  @get: Internal
  lateinit var operationOutputGenerator: OperationOutputGenerator

  @Input
  fun getOperationOutputGeneratorVersion() = operationOutputGenerator.version

  @get:Input
  @get:Optional
  abstract val customTypeMapping: MapProperty<String, String>

  @get:Input
  @get:Optional
  abstract val nullableValueType: Property<String>

  @get:Input
  @get:Optional
  abstract val useSemanticNaming: Property<Boolean>

  @get:Input
  @get:Optional
  abstract val generateModelBuilder: Property<Boolean>

  @get:Input
  @get:Optional
  abstract val useJavaBeansSemanticNaming: Property<Boolean>

  @get:Input
  @get:Optional
  abstract val suppressRawTypesWarning: Property<Boolean>

  @get:Input
  @get:Optional
  abstract val generateKotlinModels: Property<Boolean>

  @get:Input
  @get:Optional
  abstract val generateVisitorForPolymorphicDatatypes: Property<Boolean>

  @get:OutputDirectory
  abstract val outputDir: DirectoryProperty

  @get:Input
  @get:Optional
  abstract val generateAsInternal: Property<Boolean>

  @get:Input
  @get:Optional
  abstract val kotlinMultiPlatformProject: Property<Boolean>

  @get:Input
  @get:Optional
  abstract val sealedClassesForEnumsMatching: ListProperty<String>

  @get:Input
  @get:Optional
  abstract val generateApolloMetadata: Property<Boolean>

  private data class SchemaInfo(val introspectionSchema: IntrospectionSchema, val schemaPackageName: String)

  private fun getSchemaInfo(roots: Roots, metadata: ApolloMetadata?): SchemaInfo {
    check(schemaFile.isPresent.or(metadata != null)) {
      "ApolloGraphQL: cannot find schema.[json | sdl]"
    }
    check(schemaFile.isPresent.and(metadata != null).not()) {
      "ApolloGraphQL: You can't define a schema in ${schemaFile.get().asFile.absolutePath} as one is already defined in a dependency. " +
          "Either remove the schema or the dependency"
    }

    if (metadata == null) {
      val realSchemaFile = schemaFile.get().asFile

      val introspectionSchema = if (realSchemaFile.extension == "json") {
        IntrospectionSchema(realSchemaFile)
      } else {
        GraphSdlSchema(realSchemaFile).toIntrospectionSchema()
      }

      val schemaPackageName = try {
        roots.filePackageName(realSchemaFile.absolutePath)
      } catch (e: IllegalArgumentException) {
        // Can happen if the schema is not a child of roots
        ""
      }
      return SchemaInfo(introspectionSchema, schemaPackageName)
    } else {
      return SchemaInfo(metadata.schema, metadata.options.schemaPackageName)
    }
  }

  @TaskAction
  fun taskAction() {
    checkParameters()

    val output = outputDir.get()
    output.asFile.deleteRecursively()
    output.asFile.mkdirs()

    val roots = Roots(rootFolders.get().map { project.file(it) })
    val generateMetadata = generateApolloMetadata.orElse(false).get()

    val metadata = getMetadata()
    val (introspectionSchema, schemaPackageName) = getSchemaInfo(roots, metadata)

    val packageNameProvider = DefaultPackageNameProvider(
        roots = roots,
        rootPackageName = rootPackageName.getOrElse(""),
        schemaPackageName = schemaPackageName
    )

    val files = graphqlFiles.files
    checkDuplicateFiles(roots, files)

    val codeGenerationIR = GraphQLDocumentParser(
        schema = introspectionSchema,
        packageNameProvider = packageNameProvider,
        inheritedFragments = metadata?.fragments ?: emptyList(),
        exportAllTypes = generateMetadata
    ).parse(files)

    val operationOutput = codeGenerationIR.operations.map {
      OperationDescriptor(
          name = it.operationName,
          packageName = it.packageName,
          filePath = it.filePath,
          source = QueryDocumentMinifier.minify(it.sourceWithFragments)
      )
    }.let {
      operationOutputGenerator.generate(it)
    }
    if (operationOutputFile.isPresent()) {
      operationOutputFile.get().asFile.writeText(operationOutput.toJson("  "))
    }

    if (generateMetadata) {
      val metadataDir = metadataOutputDir.asFile.get()
      metadataDir.deleteRecursively()
      metadataDir.mkdirs()

      val moshi = Moshi.Builder().build()
      if (metadata == null) {
        // do not write the schema if we have incoming metadata, this ensures there is a single source of truth for the schema
        File(metadataDir, "schema.json").sink().buffer().use {
          introspectionSchema.toJson(it)
        }
      }
      File(metadataDir, "options.json").sink().buffer().use {
        val options = MetadataOptions(schemaPackageName = schemaPackageName)
        moshi.adapter(MetadataOptions::class.java).toJson(it, options)
      }
      File(metadataDir, "fragments.json").sink().buffer().use {
        val type = Types.newParameterizedType(List::class.java, Fragment::class.java)
        // Remove the path to support build cache.
        val fragments = codeGenerationIR.fragments.map { it.copy(filePath = null) }
        moshi.adapter<List<Fragment>>(type).toJson(it, fragments)
      }
    }

    val nullableValueTypeEnum = NullableValueType.values().find { it.value == nullableValueType.getOrElse(NullableValueType.ANNOTATED.value) }
    if (nullableValueTypeEnum == null) {
      throw IllegalArgumentException("ApolloGraphQL: Unknown nullableValueType: '${nullableValueType.get()}'. Possible values:\n" +
          NullableValueType.values().joinToString(separator = "\n") { it.value })
    }

    check(operationOutput.size == codeGenerationIR.operations.size) {
      """The number of operation IDs (${operationOutput.size}) should match the number of operations (${codeGenerationIR.operations.size}).
        |Check that all your IDs are unique.
      """.trimMargin()
    }
    val args = GraphQLCompiler.Arguments(
        ir = codeGenerationIR,
        outputDir = output.asFile,
        customTypeMap = customTypeMapping.getOrElse(emptyMap()),
        operationOutput = operationOutput,
        nullableValueType = nullableValueTypeEnum,
        useSemanticNaming = useSemanticNaming.getOrElse(true),
        generateModelBuilder = generateModelBuilder.getOrElse(false),
        useJavaBeansSemanticNaming = useJavaBeansSemanticNaming.getOrElse(false),
        suppressRawTypesWarning = suppressRawTypesWarning.getOrElse(false),
        generateKotlinModels = generateKotlinModels.getOrElse(false),
        generateVisitorForPolymorphicDatatypes = generateVisitorForPolymorphicDatatypes.getOrElse(false),
        generateAsInternal = generateAsInternal.getOrElse(false),
        kotlinMultiPlatformProject = kotlinMultiPlatformProject.getOrElse(false),
        enumAsSealedClassPatternFilters = sealedClassesForEnumsMatching.getOrElse(emptyList()),
        writeTypes = metadata == null // If we have incoming metadata, skip writing the fragments and types
    )

    GraphQLCompiler().write(args)
  }

  private fun checkParameters() {
    if (generateKotlinModels.getOrElse(false) && generateModelBuilder.getOrElse(false)) {
      throw IllegalArgumentException("""
        ApolloGraphQL: Using `generateModelBuilder = true` does not make sense with `generateKotlinModels = true`. You can use .copy() as models are data classes.
      """.trimIndent())
    }

    if (generateKotlinModels.getOrElse(false) && useJavaBeansSemanticNaming.getOrElse(false)) {
      throw IllegalArgumentException("""
        ApolloGraphQL: Using `useJavaBeansSemanticNaming = true` does not make sense with `generateKotlinModels = true`
      """.trimIndent())
    }

    if (generateKotlinModels.getOrElse(false) && nullableValueType.isPresent) {
      throw IllegalArgumentException("""
        ApolloGraphQL: Using `nullableValueType` does not make sense with `generateKotlinModels = true`
      """.trimIndent())
    }
  }


  private fun getMetadata(): ApolloMetadata? {
    val zips = metadataConfiguration.incoming.artifacts.artifacts.map { it.file }
    check(zips.size <= 1) {
      "Cannot choose between metadataZips for configuration: ${metadataConfiguration.name}\n" +
          zips.map { it.absolutePath }.joinToString("\n")
    }
    if (zips.isEmpty()) {
      return null
    }

    val zipFile = ZipFile(zips.first())

    val schema = zipFile.getEntry("metadata/schema.json").let { zipEntry ->
      zipFile.getInputStream(zipEntry).use { inputStream ->
        IntrospectionSchema(inputStream, "from metadata/schema.json")
      }
    }

    val options = zipFile.getEntry("metadata/options.json").let { zipEntry ->
      zipFile.getInputStream(zipEntry).use { inputStream ->
        MetadataOptions(inputStream)
      }
    }

    val fragments = zipFile.getEntry("metadata/fragments.json").let { zipEntry ->
      zipFile.getInputStream(zipEntry).use { inputStream ->
        val type = Types.newParameterizedType(List::class.java, Fragment::class.java)
        Moshi.Builder().build().adapter<List<Fragment>>(type).fromJson(inputStream.source().buffer())
      }
    }
    check(fragments != null) {
      "Apollo: cannot parse fragments from fragments.json"
    }

    return ApolloMetadata(schema, options, fragments)
  }

  companion object {
    /**
     * Check for duplicates files. This can happen with Android variants
     */
    private fun checkDuplicateFiles(roots: Roots, files: Set<File>) {
      val map = files.groupBy { roots.filePackageName(it.normalize().absolutePath) to it.nameWithoutExtension }

      map.values.forEach {
        require(it.size == 1) {
          "ApolloGraphQL: duplicate(s) graphql file(s) found:\n" +
              it.map { it.absolutePath }.joinToString("\n")
        }
      }
    }
  }
}
