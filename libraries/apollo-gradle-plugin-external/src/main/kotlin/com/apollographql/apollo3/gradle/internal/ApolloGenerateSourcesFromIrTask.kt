package com.apollographql.apollo3.gradle.internal

import com.apollographql.apollo3.compiler.ApolloCompiler
import com.apollographql.apollo3.compiler.toCodegenOptions
import com.apollographql.apollo3.compiler.toCodegenSchema
import com.apollographql.apollo3.compiler.toCodegenSymbols
import com.apollographql.apollo3.compiler.toIrOperations
import com.apollographql.apollo3.compiler.writeTo
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File

@CacheableTask
abstract class ApolloGenerateSourcesFromIrTask : ApolloGenerateSourcesBaseTask() {
  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val codegenSchemaFiles: ConfigurableFileCollection

  @get:InputFile
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val irOperations: RegularFileProperty

  @get:Input
  abstract val usedCoordinates: MapProperty<String, Set<String>>

  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val upstreamCodegenSymbols: ConfigurableFileCollection

  @get:OutputFile
  abstract val codegenSymbols: RegularFileProperty

  @TaskAction
  fun taskAction() {
    val codegenSchema = codegenSchemaFiles.findCodegenSchemaFile().toCodegenSchema()
    val packageNameGenerator = packageNameGenerator(packageName = packageName, rootPackageName = rootPackageName, packageNameRoots = packageNameRoots!!)
    val irOperations = irOperations.get().asFile.toIrOperations()
    val upstreamCodegenSymbols = upstreamCodegenSymbols.map { it.toCodegenSymbols() }
    val codegenOptions = codegenOptionsFile.get().asFile.toCodegenOptions()

    val plugin = compilerPlugin()

    ApolloCompiler.buildSchemaAndOperationsSourcesFromIr(
        codegenSchema = codegenSchema,
        irOperations = irOperations,
        upstreamCodegenSymbols = upstreamCodegenSymbols,
        usedCoordinates = usedCoordinates.get(),
        codegenOptions = codegenOptions,
        packageNameGenerator = packageNameGenerator,
        javaOutputTransform = plugin?.javaOutputTransform(),
        kotlinOutputTransform = plugin?.kotlinOutputTransform()
    ).writeTo(
        outputDir.get().asFile,
        true,
        codegenSymbols.get().asFile
    )
  }

  companion object {
    fun Iterable<File>.findCodegenSchemaFile(): File {
      return singleOrNull() ?: error("Multiple CodegenSchemas found. Check your multi-module configuration")
    }
  }
}
