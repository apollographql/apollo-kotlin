package com.apollographql.apollo.gradle.internal

import com.apollographql.apollo.compiler.*
import com.apollographql.apollo.compiler.DefaultPackageNameProvider
import com.apollographql.apollo.compiler.parser.GraphQLDocumentParser
import com.apollographql.apollo.compiler.parser.Schema
import org.gradle.api.DefaultTask
import org.gradle.api.file.*
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import java.io.File

@CacheableTask
abstract class ApolloGenerateSourcesTask : DefaultTask() {
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
  @get:OutputDirectory
  abstract val transformedQueriesOutputDir: DirectoryProperty

  @get:Optional
  @get:OutputDirectory
  abstract val operationOutputDir: DirectoryProperty

  @get:Input
  @get:Optional
  abstract val generateAsInternal: Property<Boolean>

  @TaskAction
  fun taskAction() {

    val realSchemaFile = schemaFile.get().asFile

    outputDir.get().asFile.delete()

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
          NullableValueType.values().map { it.value }.joinToString("\n"))
    }

    val codeGenerationIR = GraphQLDocumentParser(schema, packageNameProvider).parse(files)
    val args = GraphQLCompiler.Arguments(
        ir = codeGenerationIR,
        outputDir = outputDir.get().asFile,
        customTypeMap = customTypeMapping.getOrElse(emptyMap()),
        nullableValueType = nullableValueTypeEnum,
        useSemanticNaming = useSemanticNaming.getOrElse(true),
        generateModelBuilder = generateModelBuilder.getOrElse(false),
        useJavaBeansSemanticNaming = useJavaBeansSemanticNaming.getOrElse(false),
        suppressRawTypesWarning = suppressRawTypesWarning.getOrElse(false),
        generateKotlinModels = generateKotlinModels.getOrElse(false),
        generateVisitorForPolymorphicDatatypes = generateVisitorForPolymorphicDatatypes.getOrElse(false),
        packageNameProvider = packageNameProvider,
        transformedQueriesOutputDir = transformedQueriesOutputDir.orNull?.asFile,
        operationOutputDir = operationOutputDir.orNull?.asFile,
        generateAsInternal = generateAsInternal.getOrElse(false)
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
