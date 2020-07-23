package com.apollographql.apollo.gradle.internal

import com.apollographql.apollo.compiler.GraphQLCompiler
import com.apollographql.apollo.compiler.NullableValueType
import com.apollographql.apollo.compiler.ir.CodeGenerationIR
import com.apollographql.apollo.compiler.operationoutput.OperationOutput
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

@CacheableTask
abstract class ApolloGenerateSourcesTask : DefaultTask() {
  @get:InputFile
  // if no operation is defined, irFile is not written
  @get:Optional
  abstract val irFile: RegularFileProperty

  @get:InputFile
  abstract val operationOutputFile: RegularFileProperty

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
    sanityChecks()

    outputDir.get().asFile.deleteRecursively()

    if (!irFile.isPresent) {
      return
    }

    val realIRFile = irFile.asFile.get()
    val codeGenerationIR = try {
      CodeGenerationIR.fromJson(realIRFile.readText())!!
    } catch (e: Exception) {
      throw RuntimeException("Failed to parse GraphQL schema introspection query from `$realIRFile`", e)
    }

    val nullableValueTypeEnum = NullableValueType.values().find { it.value == nullableValueType.getOrElse(NullableValueType.ANNOTATED.value) }
    if (nullableValueTypeEnum == null) {
      throw IllegalArgumentException("ApolloGraphQL: Unknown nullableValueType: '${nullableValueType.get()}'. Possible values:\n" +
          NullableValueType.values().joinToString(separator = "\n") { it.value })
    }

    val operationOuput = OperationOutput(operationOutputFile.asFile.get())

    val args = GraphQLCompiler.Arguments(
        ir = codeGenerationIR,
        outputDir = outputDir.get().asFile,
        customTypeMap = customTypeMapping.getOrElse(emptyMap()),
        operationOutput = operationOuput,
        nullableValueType = nullableValueTypeEnum,
        useSemanticNaming = useSemanticNaming.getOrElse(true),
        generateModelBuilder = generateModelBuilder.getOrElse(false),
        useJavaBeansSemanticNaming = useJavaBeansSemanticNaming.getOrElse(false),
        suppressRawTypesWarning = suppressRawTypesWarning.getOrElse(false),
        generateKotlinModels = generateKotlinModels.getOrElse(false),
        generateVisitorForPolymorphicDatatypes = generateVisitorForPolymorphicDatatypes.getOrElse(false),
        generateAsInternal = generateAsInternal.getOrElse(false),
        kotlinMultiPlatformProject = kotlinMultiPlatformProject.getOrElse(false),
        enumAsSealedClassPatternFilters = sealedClassesForEnumsMatching.getOrElse(emptyList())
    )

    GraphQLCompiler().write(args)
  }

  private fun sanityChecks() {
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
