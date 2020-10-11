package com.apollographql.apollo.gradle.internal

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction

abstract class ApolloDownloadSchemaTask : DefaultTask() {
  @get:Input
  abstract val endpoint: Property<String>

  @get:Optional
  @get:Input
  var header = emptyList<String>() // cannot be lazy for @Option to work

  @get:Input
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
    val schema = project.file(schemaRelativeToProject.get())

    val introspection = SchemaDownloader.downloadIntrospection(
        endpoint = endpoint.get(),
        headers = header.toMap(),
    )

    schema.parentFile.mkdirs()
    schema.writeText(introspection)
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
