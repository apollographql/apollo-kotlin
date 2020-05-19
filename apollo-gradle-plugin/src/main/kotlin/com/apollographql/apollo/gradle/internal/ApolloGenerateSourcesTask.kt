package com.apollographql.apollo.gradle.internal

import com.apollographql.apollo.compiler.DefaultPackageNameProvider
import com.apollographql.apollo.compiler.GraphQLCompiler
import com.apollographql.apollo.compiler.NullableValueType
import com.apollographql.apollo.compiler.OperationIdGenerator
import com.apollographql.apollo.compiler.parser.GraphQLDocumentParser
import com.apollographql.apollo.compiler.parser.Schema
import org.gradle.api.DefaultTask
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

@CacheableTask
abstract class ApolloGenerateSourcesTask : DefaultTask() {
  @get:Input
  @get:Optional
  abstract val customTypeMapping: MapProperty<String, String>

  @get:Internal
  abstract val operationIdGenerator: Property<OperationIdGenerator>

  @Input
  @Optional
  fun getOperationIdGeneratorVersion() = operationIdGenerator.orNull?.version

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

  @get:Input
  @get:Optional
  abstract val rootPackageName: Property<String>

  @get:InputFiles
  @get:SkipWhenEmpty
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val graphqlFiles: ConfigurableFileCollection

  @get:InputFile
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val schemaFile: RegularFileProperty

  @get:Input
  abstract val rootFolders: ListProperty<String>

  @get:OutputDirectory
  abstract val outputDir: DirectoryProperty

  @get:Optional
  @get:OutputFile
  abstract val operationOutputFile: RegularFileProperty

  @get:Input
  @get:Optional
  abstract val generateAsInternal: Property<Boolean>

  @get:Input
  @get:Optional
  abstract val kotlinMultiPlatformProject: Property<Boolean>

  @get:Input
  @get:Optional
  abstract val sealedClassesForEnumsMatching: ListProperty<String>

  @TaskAction
  fun taskAction() {

    val realSchemaFile = schemaFile.get().asFile

    outputDir.get().asFile.deleteRecursively()

    val schema = Schema.invoke(realSchemaFile)

    val packageNameProvider = DefaultPackageNameProvider(
        rootFolders = rootFolders.get(),
        rootPackageName = rootPackageName.getOrElse(""),
        schemaFile = realSchemaFile
    )

    val files = graphqlFiles.files
    sanityChecks(packageNameProvider, files)

    val nullableValueTypeEnum = NullableValueType.values().find { it.value == nullableValueType.getOrElse(NullableValueType.ANNOTATED.value) }
    if (nullableValueTypeEnum == null) {
      throw IllegalArgumentException("ApolloGraphQL: Unknown nullableValueType: '${nullableValueType.get()}'. Possible values:\n" +
          NullableValueType.values().joinToString(separator = "\n") { it.value })
    }

    val codeGenerationIR = GraphQLDocumentParser(schema, packageNameProvider).parse(files)
    val args = GraphQLCompiler.Arguments(
        ir = codeGenerationIR,
        outputDir = outputDir.get().asFile,
        customTypeMap = customTypeMapping.getOrElse(emptyMap()),
        operationIdGenerator = operationIdGenerator.getOrElse(OperationIdGenerator.Sha256()),
        nullableValueType = nullableValueTypeEnum,
        useSemanticNaming = useSemanticNaming.getOrElse(true),
        generateModelBuilder = generateModelBuilder.getOrElse(false),
        useJavaBeansSemanticNaming = useJavaBeansSemanticNaming.getOrElse(false),
        suppressRawTypesWarning = suppressRawTypesWarning.getOrElse(false),
        generateKotlinModels = generateKotlinModels.getOrElse(false),
        generateVisitorForPolymorphicDatatypes = generateVisitorForPolymorphicDatatypes.getOrElse(false),
        packageNameProvider = packageNameProvider,
        operationOutputFile = operationOutputFile.orNull?.asFile,
        generateAsInternal = generateAsInternal.getOrElse(false),
        kotlinMultiPlatformProject = kotlinMultiPlatformProject.getOrElse(false),
        enumAsSealedClassPatternFilters = sealedClassesForEnumsMatching.getOrElse(emptyList())
    )

    GraphQLCompiler().write(args)
  }

  private fun sanityChecks(packageNameProvider: DefaultPackageNameProvider, files: Set<File>) {
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

    val map = files.groupBy { packageNameProvider.filePackageName(it.normalize().absolutePath) to it.nameWithoutExtension }

    map.values.forEach {
      require(it.size == 1) {
        "ApolloGraphQL: duplicate(s) graphql file(s) found:\n" +
            it.map { it.absolutePath }.joinToString("\n")
      }
    }
  }
}
