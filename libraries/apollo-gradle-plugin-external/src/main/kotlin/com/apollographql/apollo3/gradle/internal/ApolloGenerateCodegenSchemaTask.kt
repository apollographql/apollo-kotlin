package com.apollographql.apollo3.gradle.internal

import com.apollographql.apollo3.compiler.ApolloCompiler
import com.apollographql.apollo3.compiler.toCodegenSchemaOptions
import com.apollographql.apollo3.compiler.writeTo
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import com.apollographql.apollo3.compiler.InputFile as ApolloInputFile

@CacheableTask
abstract class ApolloGenerateCodegenSchemaTask : DefaultTask() {
  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val schemaFiles: ConfigurableFileCollection

  @Internal
  var sourceRoots: Set<String>? = null

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

    val normalizedSchemaFiles = (schemaFiles.files.takeIf { it.isNotEmpty() }?: fallbackSchemaFiles.files).map {
      // this may produce wrong cache results as that computation is not the same as the Gradle normalization
      ApolloInputFile(it, it.normalizedPath(sourceRoots!!))
    }

    ApolloCompiler.buildCodegenSchema(
        schemaFiles = normalizedSchemaFiles,
        logger = logger(),
        codegenSchemaOptions = codegenSchemaOptionsFile.get().asFile.toCodegenSchemaOptions(),
    ).writeTo(codegenSchemaFile.get().asFile)
  }
}
