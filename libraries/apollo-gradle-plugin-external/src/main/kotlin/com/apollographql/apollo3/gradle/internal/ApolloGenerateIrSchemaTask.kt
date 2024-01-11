package com.apollographql.apollo3.gradle.internal

import com.apollographql.apollo3.compiler.ApolloCompiler
import org.gradle.api.DefaultTask
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

@CacheableTask
abstract class ApolloGenerateIrSchemaTask : DefaultTask() {
  @get:InputFile
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val codegenSchemaFile: RegularFileProperty

  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val downStreamIrOperations: ConfigurableFileCollection

  @get:InputFile
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val irOperations: RegularFileProperty

  @get:OutputFile
  @get:Optional
  abstract val irSchemaFile: RegularFileProperty

  @TaskAction
  fun taskAction() {
    ApolloCompiler.buildIrSchema(
        codegenSchemaFile = codegenSchemaFile.get().asFile,
        irOperationsFiles = downStreamIrOperations.files.plus(irOperations.get().asFile),
        irSchemaFile = irSchemaFile.get().asFile
    )
  }
}
