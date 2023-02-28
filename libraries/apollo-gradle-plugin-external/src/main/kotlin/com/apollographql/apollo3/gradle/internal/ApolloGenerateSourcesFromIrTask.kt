package com.apollographql.apollo3.gradle.internal

import com.apollographql.apollo3.compiler.CodegenSchema
import com.apollographql.apollo3.compiler.ir.toIrOperations
import com.apollographql.apollo3.compiler.toCodegenSchema
import com.apollographql.apollo3.compiler.toCodegenMetadata
import com.apollographql.apollo3.compiler.toUsedCoordinates
import com.apollographql.apollo3.compiler.writeTo
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
import java.io.File

@Suppress("UnstableApiUsage") // Because the gradle-api we link against has a lot of symbols still experimental
@CacheableTask
abstract class ApolloGenerateSourcesFromIrTask : ApolloGenerateSourcesBase() {
  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val codegenSchemas: ConfigurableFileCollection

  @get:InputFile
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val irOperations: RegularFileProperty

  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val upstreamMetadata: ConfigurableFileCollection

  @get:InputFile
  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:Optional
  abstract val usedCoordinates: RegularFileProperty

  @get:OutputFile
  @get:Optional
  abstract val metadataOutputFile: RegularFileProperty

  @TaskAction
  fun taskAction() {

    val codegenSchema = codegenSchemas.files.findCodegenSchema()
    val irOperations = irOperations.asFile.get().toIrOperations()

    val usedCoordinates = usedCoordinates.orNull?.asFile?.toUsedCoordinates()

    val codegenMetadata = runCodegen(
        codegenSchema = codegenSchema,
        irOperations = irOperations,
        usedCoordinates = usedCoordinates,
        upstreamMetadata = upstreamMetadata.files.map { it.toCodegenMetadata() }
    )

    codegenMetadata.writeTo(metadataOutputFile.get().asFile)
  }

  companion object {
    fun Iterable<File>.findCodegenSchema(): CodegenSchema {
      return firstOrNull {
        it.length() > 0
      }?.toCodegenSchema() ?: error("Cannot find CodegenSchema in $this")
    }
  }
}
