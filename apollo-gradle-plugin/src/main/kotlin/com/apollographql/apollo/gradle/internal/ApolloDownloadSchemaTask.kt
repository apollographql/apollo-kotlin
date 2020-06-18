package com.apollographql.apollo.gradle.internal

import org.gradle.api.DefaultTask
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import java.io.File

abstract class ApolloDownloadSchemaTask : DefaultTask() {
  @get:Input
  @get:Option(option = "endpoint", description = "url of the GraphQL endpoint")
  abstract val endpoint: Property<String>

  @get:Input
  @get:Optional
  @get:Option(option = "schema", description = "path where the schema will be downloaded, relative to the current working directory")
  abstract val schema: Property<String>

  @get:Optional
  @get:Input
  @set:Option(option = "header", description = "headers in the form 'Name: Value'")
  var header = emptyList<String>() // cannot be lazy for @Option to work

  @get:Input
  @get:Optional
  @get:Option(option = "schemaRelativeToProject", description = "path where the schema will be downloaded, relative to the current working directory")
  abstract val schemaRelativeToProject: Property<String>

  init {
    /**
     * We cannot know in advance if the backend schema changed so don't cache or mark this task up-to-date
     * This code actually redundant because the task has no output but adding it make it explicit.
     */
    outputs.upToDateWhen { false }
    outputs.cacheIf { false }
  }

  @TaskAction
  fun taskAction() {
    val schema = when {
      schema.isPresent -> File(schema.get())
      schemaRelativeToProject.isPresent -> project.file(schemaRelativeToProject.get())
      else -> throw IllegalArgumentException("schema or schemaRelativeToProject is required")
    }

    SchemaDownloader.download(
        endpoint = endpoint.get(),
        schema = schema,
        headers = header.toMap(),
        connectTimeoutSeconds = System.getProperty("okHttp.connectTimeout", "600").toLong(),
        readTimeoutSeconds = System.getProperty("okHttp.readTimeout", "600").toLong()
    )
  }

  private fun List<String>.toMap(): Map<String, String> {
    return map {
      val index = it.indexOf(':')
      check(index > 0 && index < it.length - 1) {
        "header should be in the form 'Name: Value'"
      }

      it.substring(0, index).trim() to it.substring(index + 1, it.length).trim()
    }.toMap()
  }
}
