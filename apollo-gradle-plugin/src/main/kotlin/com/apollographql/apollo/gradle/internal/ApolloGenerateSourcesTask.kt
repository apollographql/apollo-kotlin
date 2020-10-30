package com.apollographql.apollo.gradle.internal

import com.apollographql.apollo.compiler.GraphQLCompiler
import com.apollographql.apollo.compiler.NullableValueType
import com.apollographql.apollo.compiler.OperationOutputGenerator
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
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
import javax.inject.Inject

@CacheableTask
abstract class ApolloGenerateSourcesTask : DefaultTask() {
  @get:OutputFile
  @get:Optional
  abstract val operationOutputFile: RegularFileProperty

  @get:OutputFile
  abstract val metadataOutputFile: RegularFileProperty

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
  abstract val metadataFiles: ConfigurableFileCollection

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
  abstract val packageName: Property<String>

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

  @get:Internal
  abstract val warnOnDeprecatedUsages: Property<Boolean>

  @get:Internal
  abstract val failOnWarnings: Property<Boolean>

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

  @get:Inject
  abstract val objectFactory: ObjectFactory

  @get:Input
  abstract val projectName: Property<String>

  @get:Internal // The generated models do not depend on the location on the project
  abstract val projectRootDir: DirectoryProperty

  @TaskAction
  fun taskAction() {
    checkParameters()

    val nullableValueTypeEnum = NullableValueType.values().find { it.value == nullableValueType.getOrElse(NullableValueType.ANNOTATED.value) }
    if (nullableValueTypeEnum == null) {
      throw IllegalArgumentException("ApolloGraphQL: Unknown nullableValueType: '${nullableValueType.get()}'. Possible values:\n" +
          NullableValueType.values().joinToString(separator = "\n") { it.value })
    }

    val args = GraphQLCompiler.Arguments(
        rootFolders = objectFactory.fileCollection().from(rootFolders).files.toList(),
        graphqlFiles = graphqlFiles.files,
        schemaFile = schemaFile.asFile.orNull,
        outputDir = outputDir.asFile.get(),

        metadata = metadataFiles.files.toList(),
        metadataOutputFile = metadataOutputFile.asFile.get(),
        generateMetadata = generateMetadata.getOrElse(false),
        alwaysGenerateTypesMatching = alwaysGenerateTypesMatching.orNull,
        moduleName = projectName.get(),
        rootProjectDir = projectRootDir.get().asFile,
        packageName = packageName.orNull,

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
        warnOnDeprecatedUsages = warnOnDeprecatedUsages.getOrElse(true),
        failOnWarnings = failOnWarnings.getOrElse(false),
        generateVisitorForPolymorphicDatatypes = generateVisitorForPolymorphicDatatypes.getOrElse(false),
        generateAsInternal = generateAsInternal.getOrElse(false),
        kotlinMultiPlatformProject = kotlinMultiPlatformProject.getOrElse(false),
        enumAsSealedClassPatternFilters = sealedClassesForEnumsMatching.getOrElse(emptyList()).toSet(),

    )

    val logger = object :GraphQLCompiler.Logger {
      override fun warning(message: String) {
        logger.lifecycle(message)
      }
    }

    GraphQLCompiler(logger).write(args)
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
