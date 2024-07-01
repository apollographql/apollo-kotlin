// because of PackageNameGenerator and OperationOutputGenerator
@file:Suppress("DEPRECATION")

package com.apollographql.apollo.gradle.internal

import com.apollographql.apollo.compiler.CodegenSchema
import com.apollographql.apollo.compiler.LayoutFactory
import com.apollographql.apollo.compiler.OperationOutputGenerator
import com.apollographql.apollo.compiler.PackageNameGenerator
import com.apollographql.apollo.compiler.codegen.SchemaAndOperationsLayout
import com.apollographql.apollo.compiler.toCodegenOptions
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.workers.WorkerExecutor
import javax.inject.Inject

abstract class ApolloGenerateSourcesBaseTask : ApolloTaskWithClasspath() {
  @get:InputFile
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val codegenOptionsFile: RegularFileProperty

  @get:Internal
  var packageNameGenerator: PackageNameGenerator? = null

  @Input
  fun getPackageNameGeneratorVersion() = packageNameGenerator?.version ?: ""

  @get:Internal
  var operationOutputGenerator: OperationOutputGenerator? = null

  @Input
  fun getOperationOutputGeneratorVersion() = operationOutputGenerator?.version ?: ""

  @get:OutputFile
  @get:Optional
  abstract val operationManifestFile: RegularFileProperty

  @get:OutputDirectory
  abstract val outputDir: DirectoryProperty

  @Inject
  abstract fun getWorkerExecutor(): WorkerExecutor
}

fun ApolloGenerateSourcesBaseTask.layout(): LayoutFactory {
  return object : LayoutFactory {
    override fun create(codegenSchema: CodegenSchema): SchemaAndOperationsLayout? {
      return if (packageNameGenerator == null) {
        null
      } else {
        val options = codegenOptionsFile.get().asFile.toCodegenOptions()
        SchemaAndOperationsLayout(codegenSchema, packageNameGenerator!!, options.useSemanticNaming, options.decapitalizeFields, options.generatedSchemaName)
      }
    }
  }
}

fun ApolloGenerateSourcesBaseTask.requiresBuildscriptClasspath(): Boolean {
  if (packageNameGenerator != null || operationOutputGenerator != null) {
    if (packageNameGenerator != null) {
      logger.lifecycle("Apollo: packageNameGenerator is deprecated, use Apollo compiler plugins instead. See https://go.apollo.dev/ak-compiler-plugins for more details.")
    }
    if (operationOutputGenerator != null) {
      logger.lifecycle("Apollo: operationOutputGenerator is deprecated, use Apollo compiler plugins instead. See https://go.apollo.dev/ak-compiler-plugins for more details.")
    }
    check(!hasPlugin.get()) {
      "Apollo: using ApolloCompilerPlugin and operationOutputGenerator/operationIdGenerator/packageNameGenerator at the same time is not supported. See https://go.apollo.dev/ak-compiler-plugins for more details."
    }

    return true
  }

  return false
}