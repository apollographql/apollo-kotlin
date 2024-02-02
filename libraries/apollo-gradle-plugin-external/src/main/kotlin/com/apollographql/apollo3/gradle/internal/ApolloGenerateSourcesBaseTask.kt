package com.apollographql.apollo3.gradle.internal

import com.apollographql.apollo3.compiler.CodegenSchema
import com.apollographql.apollo3.compiler.LayoutFactory
import com.apollographql.apollo3.compiler.OperationOutputGenerator
import com.apollographql.apollo3.compiler.PackageNameGenerator
import com.apollographql.apollo3.compiler.codegen.SchemaAndOperationsLayout
import com.apollographql.apollo3.compiler.hooks.ApolloCompilerJavaHooks
import com.apollographql.apollo3.compiler.hooks.ApolloCompilerKotlinHooks
import com.apollographql.apollo3.compiler.toCodegenOptions
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Classpath
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

abstract class ApolloGenerateSourcesBaseTask : DefaultTask() {
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

  @get:Internal
  var compilerKotlinHooks: List<ApolloCompilerKotlinHooks>? = null

  @Input
  fun getCompilerKotlinHooksVersion() = compilerKotlinHooks.orEmpty().map { it.version }.joinToString()

  @get:Internal
  var compilerJavaHooks: List<ApolloCompilerJavaHooks>? = null

  @Input
  fun getCompilerJavaHooksVersion() = compilerKotlinHooks.orEmpty().map { it.version }.joinToString()

  @get:OutputFile
  @get:Optional
  abstract val operationManifestFile: RegularFileProperty

  @get:OutputDirectory
  abstract val outputDir: DirectoryProperty

  @get:Classpath
  abstract val classpath: ConfigurableFileCollection

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
  if (packageNameGenerator != null || operationOutputGenerator != null || compilerJavaHooks != null || compilerKotlinHooks != null) {
    if (packageNameGenerator != null) {
      logger.lifecycle("Apollo: packageNameGenerator is deprecated, use Apollo compiler plugins instead")
    }
    if (operationOutputGenerator != null) {
      logger.lifecycle("Apollo: operationOutputGenerator is deprecated, use Apollo compiler plugins instead")
    }
    if (compilerJavaHooks != null) {
      logger.lifecycle("Apollo: compilerJavaHooks is deprecated, use Apollo compiler plugins instead")
    }
    if (compilerKotlinHooks != null) {
      logger.lifecycle("Apollo: compilerKotlinHooks is deprecated, use Apollo compiler plugins instead")
    }

    return true
  }

  return false
}