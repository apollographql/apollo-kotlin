package com.apollographql.apollo3.gradle.internal

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import java.io.File

abstract class ApolloPushSchemaTask : DefaultTask() {
  @get:Input
  @get:Optional
  @get:Option(option = "schema", description = "schema to push as SDL")
  abstract val schema: Property<String>

  @get:Optional
  @get:Input
  @get:Option(option = "key", description = "The Apollo API key. See https://www.apollographql.com/docs/studio/api-keys/ for more information on how to get your API key.")
  abstract val key: Property<String>

  @get:Optional
  @get:Input
  @get:Option(option = "graph", description = "The identifier of the Apollo graph used to download the schema.")
  abstract val graph: Property<String>

  @get:Optional
  @get:Input
  @get:Option(option = "graphVariant", description = "The variant of the Apollo graph used to download the schema.")
  abstract val graphVariant: Property<String>


  private fun Property<String>.orProperty(name: String) = orElse(project.provider {
    (project.findProperty("com.apollographql.apollo3.$name") as? String)?.also {
      logger.lifecycle("Using the com.apollographql.apollo3.$name property is deprecated. Use --$name instead.")
    }
  }).orNull

  @TaskAction
  fun taskAction() {
    val key = key.orProperty("key")
    var graph = graph.orProperty("graph")
    val graphVariant = graphVariant.orProperty("graph-variant")
    val schema = schema.orNull

    check (key != null) {
      "please define key"
    }

    if (graph == null && key.startsWith("service:")) {
      graph = key.split(":")[1]
    }

    check (graph != null) {
      "please define graph"
    }

    check (schema != null) {
      "please define schema"
    }

    SchemaUploader.uploadSchema(key = key, graph = graph, variant = graphVariant ?: "current", File(schema).readText())
  }
}