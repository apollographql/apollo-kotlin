package com.apollographql.apollo3.gradle.internal

import com.apollographql.apollo3.ast.introspection.toSchema
import com.apollographql.apollo3.compiler.ApolloCompiler
import com.apollographql.apollo3.compiler.IncomingOptions.Companion.resolveSchema
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

@CacheableTask
abstract class ApolloGenerateUsedCoordinatesTask : DefaultTask() {
  @get:OutputFile
  @get:Optional
  abstract val outputFile: RegularFileProperty

  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val graphqlFiles: ConfigurableFileCollection

  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val schemaFiles: ConfigurableFileCollection

  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val incomingSchemaFiles: ConfigurableFileCollection

  @get:Input
  abstract val rootFolders: ListProperty<String>


  @TaskAction
  fun taskAction() {
    try {
      val schemas = incomingSchemaFiles.files.toList().map { it.toSchema() }

      check(schemas.size <= 1) {
        "Apollo: multiple incoming schemas"
      }

      var schema = schemas.singleOrNull()
      if (schema == null) {
        schema = resolveSchema(schemaFiles.files, rootFolders.get()).first
      }

      ApolloCompiler.writeUsedCoordinates(schema, graphqlFiles.files, outputFile.get().asFile)
    } catch (e: Exception) {
      outputFile.get().asFile.writeText("[]")
    }
  }
}
