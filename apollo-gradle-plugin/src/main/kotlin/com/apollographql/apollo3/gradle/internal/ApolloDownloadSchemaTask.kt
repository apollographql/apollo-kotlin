package com.apollographql.apollo3.gradle.internal

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.ast.GQLDocument
import com.apollographql.apollo3.ast.parseAsGQLDocument
import com.apollographql.apollo3.ast.toSchema
import com.apollographql.apollo3.ast.toUtf8
import com.apollographql.apollo3.ast.validateAsSchema
import com.apollographql.apollo3.compiler.introspection.IntrospectionSchema
import com.apollographql.apollo3.compiler.introspection.toGQLDocument
import com.apollographql.apollo3.compiler.introspection.toIntrospectionSchema
import com.apollographql.apollo3.compiler.toJson
import okio.Buffer
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import java.io.File

/**
 * A task to download a schema either from introspection or from the registry.
 *
 * This task can either be configured from the command line or from the gradle scripts
 */
@OptIn(ApolloExperimental::class)
abstract class ApolloDownloadSchemaTask : DefaultTask() {
  @get:Optional
  @get:Input
  @get:Option(option = "endpoint", description = "url of the GraphQL endpoint")
  abstract val endpoint: Property<String>

  @get:Optional
  @get:Input
  @get:Option(option = "graph", description = "The identifier of the Apollo graph used to download the schema.")
  abstract val graph: Property<String>

  @get:Optional
  @get:Input
  @get:Option(option = "key", description = "The Apollo API key. See https://www.apollographql.com/docs/studio/api-keys/ for more information on how to get your API key.")
  abstract val key: Property<String>

  @get:Optional
  @get:Input
  @get:Option(option = "graphVariant", description = "The variant of the Apollo graph used to download the schema.")
  abstract val graphVariant: Property<String>

  @get:Optional
  @get:Input
  @get:Option(option = "registryUrl", description = "The registry url of the registry instance used to download the schema. Defaults to \"https://graphql.api.apollographql.com/api/graphql\"")
  abstract val registryUrl: Property<String>

  @get:Input
  @get:Optional
  @get:Option(option = "schema", description = "path where the schema will be downloaded, relative to the current working directory")
  abstract val schema: Property<String>

  @get:Optional
  @get:Input
  @set:Option(option = "header", description = "headers in the form 'Name: Value'")
  var header = emptyList<String>() // cannot be abstract for @Option to work

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

    val endpointUrl = endpoint.orNull

    val schema = schema.orNull?.let { File(it) } // commandline is resolved relative to cwd
    check(schema != null) {
      "Apollo: no schema property"
    }
    val headers = header.toMap()

    var introspectionSchema: IntrospectionSchema? = null
    var gqlSchema: GQLDocument? = null

    val key = key.orNull
    var graph = graph.orNull
    val graphVariant = graphVariant.orNull

    if (graph == null && key != null && key.startsWith("service:")) {
      // Fallback to reading the graph from the key
      // This will not work with user keys
      graph = key.split(":")[1]
    }

    when {
      endpointUrl != null -> {
        introspectionSchema = SchemaDownloader.downloadIntrospection(
            endpoint = endpointUrl,
            headers = headers,
        ).toIntrospectionSchema()
      }
      graph != null -> {
        check (key != null) {
          "Apollo: please define --key to download graph $graph"
        }
        gqlSchema = SchemaDownloader.downloadRegistry(
            graph = graph,
            key = key,
            variant = graphVariant ?: "current",
            endpoint = registryUrl.orNull ?: "https://graphql.api.apollographql.com/api/graphql"
        ).let { Buffer().writeUtf8(it) }.parseAsGQLDocument().valueAssertNoErrors()
      }
      else -> {
        throw IllegalArgumentException("Apollo: either --endpoint or --graph is required")
      }
    }

    schema.parentFile?.mkdirs()

    if (schema.extension.lowercase() == "json") {
      if (introspectionSchema == null) {
        introspectionSchema = gqlSchema!!.validateAsSchema().valueAssertNoErrors().toIntrospectionSchema()
      }
      schema.writeText(introspectionSchema.toJson(indent = "  "))
    } else {
      if (gqlSchema == null) {
        gqlSchema = introspectionSchema!!.toGQLDocument()
      }
      schema.writeText(gqlSchema.toUtf8(indent = "  "))
    }
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
