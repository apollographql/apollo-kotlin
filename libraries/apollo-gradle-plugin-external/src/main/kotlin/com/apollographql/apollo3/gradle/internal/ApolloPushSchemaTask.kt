package com.apollographql.apollo3.gradle.internal

import com.apollographql.apollo3.gradle.internal.ApolloPlugin.Companion.extraHeaders
import com.apollographql.apollo3.tooling.SchemaUploader
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
  @get:Option(option = "schema", description = "The schema to push as SDL")
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

  @get:Optional
  @get:Input
  @get:Option(option = "subgraph", description = "The subgraph name. Can be omitted if the graph is a legacy monograph.")
  abstract val subgraph: Property<String>

  @get:Optional
  @get:Input
  @get:Option(option = "revision", description = "The revision name. Can be omitted if the graph is a legacy monograph, must be provided otherwise.")
  abstract val revision: Property<String>


  @get:Input
  abstract var projectRootDir: String

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
    val subgraph = subgraph.orNull
    val revision = revision.orNull

    check(key != null) {
      "please define key"
    }

    if (graph == null && key.startsWith("service:")) {
      graph = key.split(":")[1]
    }

    check(graph != null) {
      "please define graph"
    }

    check(schema != null) {
      "please define schema"
    }

    check(subgraph == null && revision == null || subgraph != null && revision != null) {
      "please define both subgraph and revision or neither"
    }

    // Files are relative to the root project. It is not possible in a consistent way to have them relative to the current
    // working directory where the gradle command was started
    SchemaUploader.uploadSchema(
        key = key,
        graph = graph,
        variant = graphVariant ?: "current",
        sdl = File(projectRootDir).resolve(schema).readText(),
        headers = extraHeaders,
        subgraph = subgraph,
        revision = revision,
    )
  }
}
