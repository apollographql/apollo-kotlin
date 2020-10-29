package com.apollographql.apollo.gradle.internal

import com.apollographql.apollo.compiler.parser.antlr.GraphSDLParser
import com.apollographql.apollo.compiler.parser.introspection.IntrospectionSchema
import com.apollographql.apollo.compiler.parser.introspection.IntrospectionSchema.Companion.wrap
import com.apollographql.apollo.compiler.parser.introspection.toSDL
import com.apollographql.apollo.compiler.parser.sdl.GraphSDLSchemaParser.parse
import com.apollographql.apollo.compiler.parser.sdl.toIntrospectionSchema
import com.apollographql.apollo.compiler.toJson
import okhttp3.HttpUrl.Companion.toHttpUrl
import okio.Buffer
import org.gradle.api.DefaultTask
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import java.io.File
import java.nio.charset.Charset
import java.util.Locale

/**
 * This task is very similar to [ApolloDownloadSchemaTask] except it allows to override parameters from the command line
 */
abstract class ApolloDownloadSchemaCliTask : DefaultTask() {
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

  @get:Input
  @get:Optional
  @get:Option(option = "schema", description = "path where the schema will be downloaded, relative to the current working directory")
  abstract val schema: Property<String>

  @get:Optional
  @get:Input
  @get:Option(option = "variant", description = "Variant to download the schema for. Defaults to the main variant")
  abstract val variant: Property<String>

  @get:Optional
  @get:Input
  @get:Option(option = "service", description = "Service to download the schema for. Defaults to the only service if there is only one or throws")
  abstract val service: Property<String>

  @get:Optional
  @get:Input
  @set:Option(option = "header", description = "headers in the form 'Name: Value'")
  var header = emptyList<String>() // cannot be abstract for @Option to work

  @Internal
  lateinit var compilationUnits: NamedDomainObjectContainer<DefaultCompilationUnit>

  init {
    /**
     * We cannot know in advance if the backend schema changed so don't cache or mark this task up-to-date
     * This code actually redundant because the task has no output but adding it make it explicit.
     */
    outputs.upToDateWhen { false }
    outputs.cacheIf { false }
  }

  private fun Property<String>.orProperty(name: String) = orElse(project.provider {
    (project.findProperty("com.apollographql.apollo.$name") as? String)?.also {
      logger.lifecycle("Using the com.apollographql.apollo.$name property is deprecated. Use --$name instead.")
    }
  }).orNull

  @TaskAction
  fun taskAction() {
    val candidates = compilationUnits.filter {
      if (variant.isPresent && it.variantName == variant.get()) {
        return@filter true
      }

      it.variantName == "main" || it.variantName.toLowerCase().contains("release")
    }

    val compilationUnit = if (service.isPresent) {
      candidates.firstOrNull { it.serviceName == service.get() }
    } else {
      check(candidates.size <= 1) {
        "please specify the --service"
      }
      candidates.firstOrNull()
    }

    val compilerParams = compilationUnit?.resolveParams(project)?.first

    var endpointUrl = endpoint.orProperty("endpoint") ?: compilationUnit?.service?.introspection?.endpointUrl?.get()

    val schema = schema.orProperty("schema")?.let {
      if (schema.isPresent) {
        File(it) // commandline is resolved relative to cwd
      } else {
        project.file(it) // relative to project. This is not super consistent but 🤷‍
      }
    } ?: compilerParams?.schemaFile?.asFile?.get()
      ?: throw IllegalArgumentException("ApolloGraphQL: cannot determine where to save the schema. Specify --schema or --service")

    val headersProp = project.findProperty("com.apollographql.apollo.headers") as? String
    val headers = when {
      headersProp != null -> {
        logger.lifecycle("Using the com.apollographql.apollo.headers property is deprecated. Use --header instead.")
        ApolloPlugin.toMap(headersProp)
      }
      else -> header.toMap()
    }

    val queryParamsProp = project.findProperty("com.apollographql.apollo.query_params") as? String
    if (queryParamsProp != null) {
      logger.lifecycle("Using the com.apollographql.apollo.query_params property is deprecated. Add parameters to the endpoint instead.")
      check (endpointUrl != null) {
        "ApolloGraphql: adding query_params without endpoint makes not sense. Either remove them or specify --endpoint"
      }
      endpointUrl = endpointUrl.toHttpUrl().newBuilder()
          .apply {
            ApolloPlugin.toMap(queryParamsProp).entries.forEach {
              addQueryParameter(it.key, it.value)
            }
          }
          .build()
          .toString()
    }

    var introspectionSchema: String? = null
    var sdlSchema: String? = null

    val key = key.orProperty("key")
    var graph = graph.orProperty("graph")
    val graphVariant = graphVariant.orProperty("graph-variant")

    if (graph == null && key != null && key.startsWith("service:")) {
      graph = key.split(":")[1]
    }

    if (endpointUrl != null) {
      introspectionSchema = SchemaDownloader.downloadIntrospection(
          endpoint = endpointUrl,
          headers = headers,
      )
    } else if (graph != null) {
      check (key != null) {
        "please define key"
      }
      sdlSchema = SchemaDownloader.downloadRegistry(
          graph = graph,
          key = key,
          variant = graphVariant ?: "current"
      )
    }

    schema.parentFile?.mkdirs()

    if (schema.extension.toLowerCase() == "json") {
      if (introspectionSchema == null) {
        introspectionSchema = sdlSchema!!.parse().toIntrospectionSchema().wrap().toJson()
      }
      schema.writeText(introspectionSchema)
    } else {
      if (sdlSchema == null) {
        val buffer = Buffer()
        IntrospectionSchema(introspectionSchema!!.byteInputStream()).toSDL(buffer)
        sdlSchema = buffer.readString(Charset.defaultCharset())
      }
      schema.writeText(sdlSchema)
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
