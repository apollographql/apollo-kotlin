package com.apollographql.apollo.gradle.internal

import com.apollographql.apollo.compiler.ApolloCompiler
import com.apollographql.apollo.compiler.toCodegenSchemaOptions
import com.apollographql.apollo.compiler.writeTo
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
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
import java.io.File
import javax.inject.Inject

@CacheableTask
abstract class ApolloGenerateCodegenSchemaTask : ApolloTaskWithClasspath() {
  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val schemaFiles: ConfigurableFileCollection

  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val fallbackSchemaFiles: ConfigurableFileCollection

  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val upstreamSchemaFiles: ConfigurableFileCollection

  @get:InputFile
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val codegenSchemaOptionsFile: RegularFileProperty

  @get:OutputFile
  @get:Optional
  abstract val codegenSchemaFile: RegularFileProperty

  @Inject
  abstract fun getWorkerExecutor(): WorkerExecutor

  @TaskAction
  fun taskAction() {
    if (upstreamSchemaFiles.files.isNotEmpty()) {
      /**
       * Output an empty file
       */
      codegenSchemaFile.get().asFile.let {
        it.delete()
        it.createNewFile()
      }
      return
    }

    val workQueue = getWorkerExecutor().classLoaderIsolation { workerSpec ->
      workerSpec.classpath.from(classpath)
    }

    workQueue.submit(GenerateCodegenSchema::class.java) {
      it.codegenSchemaFile = codegenSchemaFile
      it.schemaFiles = schemaFiles.isolate()
      it.fallbackSchemaFiles = fallbackSchemaFiles.isolate()
      it.codegenSchemaOptionsFile.set(codegenSchemaOptionsFile)
      it.hasPlugin = hasPlugin.get()
      it.arguments = arguments.get()
      it.logLevel = logLevel.get().ordinal
    }
  }
}


private abstract class GenerateCodegenSchema : WorkAction<GenerateCodegenSchemaParameters> {
  override fun execute() {
    with(parameters) {
      val plugin = apolloCompilerPlugin(
          arguments,
          logLevel,
          hasPlugin
      )

      val normalizedSchemaFiles = (schemaFiles.takeIf { it.isNotEmpty() }?: fallbackSchemaFiles).toInputFiles()

      ApolloCompiler.buildCodegenSchema(
          schemaFiles = normalizedSchemaFiles,
          logger = logger(),
          codegenSchemaOptions = codegenSchemaOptionsFile.get().asFile.toCodegenSchemaOptions(),
          foreignSchemas = plugin?.foreignSchemas().orEmpty()
      ).writeTo(codegenSchemaFile.get().asFile)
    }
  }
}
private interface GenerateCodegenSchemaParameters : WorkParameters {
  var codegenSchemaFile: RegularFileProperty
  var schemaFiles: List<Pair<String, File>>
  var fallbackSchemaFiles: List<Pair<String, File>>
  val codegenSchemaOptionsFile: RegularFileProperty
  var hasPlugin: Boolean
  var arguments: Map<String, Any?>
  var logLevel: Int
}

