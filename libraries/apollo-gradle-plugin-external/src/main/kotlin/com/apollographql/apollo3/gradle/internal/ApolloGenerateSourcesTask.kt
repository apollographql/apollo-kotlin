package com.apollographql.apollo3.gradle.internal

import com.apollographql.apollo3.compiler.ApolloCompiler
import com.apollographql.apollo3.compiler.PackageNameGenerator
import com.apollographql.apollo3.compiler.toCodegenOptions
import com.apollographql.apollo3.compiler.toCodegenSchemaOptions
import com.apollographql.apollo3.compiler.toIrOptions
import com.apollographql.apollo3.compiler.writeManifestTo
import com.apollographql.apollo3.compiler.writeTo
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import javax.inject.Inject


@CacheableTask
abstract class ApolloGenerateSourcesTask : ApolloGenerateSourcesBaseTask() {
  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val schemaFiles: ConfigurableFileCollection

  @get:InputFile
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val codegenSchemaOptionsFile: RegularFileProperty

  @get:InputFile
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val irOptionsFile: RegularFileProperty

  @get:Input
  abstract val operationManifestFormat: Property<String>

  @get:OutputFile
  @get:Optional
  abstract val operationManifestFile: RegularFileProperty

  @get:Classpath
  abstract val classpath: ConfigurableFileCollection

  @Inject
  abstract fun getWorkerExecutor(): WorkerExecutor

  @TaskAction
  fun taskAction() {
    val workQueue = getWorkerExecutor().classLoaderIsolation { workerSpec ->
      workerSpec.classpath.from(classpath);
    }

    workQueue.submit(GenerateSources::class.java) {
      it.graphqlFiles.from(graphqlFiles.files)
      it.codegenOptionsFile.set(codegenOptionsFile)
      it.packageName.set(packageName)
      it.rootPackageName.set(rootPackageName)
      it.packageNameRoots = packageNameRoots
      it.schemaFiles.from(schemaFiles)
      it.codegenSchemaOptionsFile.set(codegenSchemaOptionsFile)
      it.irOptionsFile.set(irOptionsFile)
      it.operationManifestFormat.set(operationManifestFormat)
      it.operationManifestFile.set(operationManifestFile)
      it.outputDir.set(outputDir)
    }
  }
}

private abstract class GenerateSources : WorkAction<GenerateSourcesParameters> {
  override fun execute() {
    val plugin = compilerPlugin()
    val packageNameGenerator: PackageNameGenerator = plugin?.packageNameGenerator() ?: packageNameGenerator(parameters.packageName, parameters.rootPackageName, parameters.packageNameRoots!!)

    val buildOutput = ApolloCompiler.buildSchemaAndOperationsSources(
        schemaFiles = parameters.schemaFiles.files,
        executableFiles = parameters.graphqlFiles.files,
        codegenSchemaOptions = parameters.codegenSchemaOptionsFile.get().asFile.toCodegenSchemaOptions(),
        irOptions = parameters.irOptionsFile.get().asFile.toIrOptions(),
        codegenOptions = parameters.codegenOptionsFile.get().asFile.toCodegenOptions(),
        packageNameGenerator = packageNameGenerator,
        operationOutputGenerator = plugin?.operationOutputGenerator(),
        logger = logger(),
        javaOutputTransform = plugin?.javaOutputTransform(),
        kotlinOutputTransform = plugin?.kotlinOutputTransform(),
        )

    buildOutput.sourceOutput.writeTo(parameters.outputDir.get().asFile, true, null)

    buildOutput.irOperations.writeManifestTo(parameters.operationManifestFile.orNull?.asFile, parameters.operationManifestFormat.get())
  }
}

private interface GenerateSourcesParameters : WorkParameters {
  val graphqlFiles: ConfigurableFileCollection

  val codegenOptionsFile: RegularFileProperty

  val packageName: Property<String>

  val rootPackageName: Property<String>

  var packageNameRoots: Set<String>?

  val schemaFiles: ConfigurableFileCollection

  val codegenSchemaOptionsFile: RegularFileProperty

  val irOptionsFile: RegularFileProperty

  val operationManifestFormat: Property<String>

  val outputDir: DirectoryProperty

  val operationManifestFile: RegularFileProperty
}