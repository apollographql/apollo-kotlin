package com.apollographql.apollo.gradle.internal

import com.apollographql.apollo.compiler.GraphQLCompiler
import com.apollographql.apollo.compiler.NullableValueType
import com.apollographql.apollo.compiler.OperationOutputGenerator
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
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
  @get:OutputFile
  @get:Optional
  abstract val operationOutputFile: RegularFileProperty

  @get:OutputDirectory
  abstract val metadataOutputDir: DirectoryProperty

  @get:Input
  abstract val generateMetadata: Property<Boolean>

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
  abstract val alwaysGenerateTypesMatching: SetProperty<String>

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

  @TaskAction
  fun taskAction() {
    checkParameters()

    val nullableValueTypeEnum = NullableValueType.values().find { it.value == nullableValueType.getOrElse(NullableValueType.ANNOTATED.value) }
    if (nullableValueTypeEnum == null) {
      throw IllegalArgumentException("ApolloGraphQL: Unknown nullableValueType: '${nullableValueType.get()}'. Possible values:\n" +
          NullableValueType.values().joinToString(separator = "\n") { it.value })
    }

    var metadataOutputDir: File? = metadataOutputDir.asFile.get()
    metadataOutputDir?.mkdirs()
    if (!generateMetadata.getOrElse(false)) {
      metadataOutputDir = null
    }

    val args = GraphQLCompiler.Arguments(
        rootFolders = rootFolders.get().map { project.file(it) },
        graphqlFiles = graphqlFiles.files,
        schemaFile = schemaFile.asFile.orNull,
        outputDir = outputDir.asFile.get(),

        metadata = metadataConfiguration.incoming.artifacts.artifacts.map { it.file },
        metadataOutputDir = metadataOutputDir,
        alwaysGenerateTypesMatching = alwaysGenerateTypesMatching.orNull,
        moduleName = project.name,
        rootProjectDir = project.rootDir,

        operationOutputFile = operationOutputFile.asFile.orNull,
        operationOutputGenerator = operationOutputGenerator,

        rootPackageName = rootPackageName.getOrElse(""),

        customTypeMap = customTypeMapping.getOrElse(emptyMap()),
        nullableValueType = nullableValueTypeEnum,
        useSemanticNaming = useSemanticNaming.getOrElse(true),
        generateModelBuilder = generateModelBuilder.getOrElse(false),
        useJavaBeansSemanticNaming = useJavaBeansSemanticNaming.getOrElse(false),
        suppressRawTypesWarning = suppressRawTypesWarning.getOrElse(false),
        generateKotlinModels = generateKotlinModels.getOrElse(false),
        generateVisitorForPolymorphicDatatypes = generateVisitorForPolymorphicDatatypes.getOrElse(false),
        generateAsInternal = generateAsInternal.getOrElse(false),
        kotlinMultiPlatformProject = kotlinMultiPlatformProject.getOrElse(false),
        enumAsSealedClassPatternFilters = sealedClassesForEnumsMatching.getOrElse(emptyList()).toSet()
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
}
