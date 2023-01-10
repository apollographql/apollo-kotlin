package com.apollographql.apollo3.gradle.internal

import com.apollographql.apollo3.ast.toSchema
import com.apollographql.apollo3.ast.toUtf8
import com.apollographql.apollo3.compiler.IncomingOptions.Companion.resolveSchema
import okio.buffer
import okio.source
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
abstract class ApolloGenerateSchemaTask : DefaultTask() {
  @get:OutputFile
  @get:Optional
  abstract val outputFile: RegularFileProperty

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
    val schemas = incomingSchemaFiles.files

    check(schemas.size <= 1) {
      "Apollo: multiple upstream schemas found"
    }

    val schemaText = try {
      val incomingSchema = schemas.singleOrNull()
      val schema = if (incomingSchema != null) {
        incomingSchema.source().buffer().use {
          it.toSchema(incomingSchema.absolutePath)
        }
      } else {
        resolveSchema(schemaFiles.files, rootFolders.get()).first
      }
      schema.toGQLDocument().toUtf8()
    } catch (e: Throwable) {
      "Could not find a schema, make sure to add dependencies to the `apolloSchema` configuration"
    }

    outputFile.get().asFile.writeText(schemaText)
  }
}
