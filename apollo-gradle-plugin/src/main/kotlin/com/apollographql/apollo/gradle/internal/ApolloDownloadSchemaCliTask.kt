package com.apollographql.apollo.gradle.internal

import com.apollographql.apollo.gradle.api.CompilationUnit
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.gradle.api.DefaultTask
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import java.io.File

/**
 * This task is very similar to [ApolloDownloadSchemaTask] except it allows to override parameters from the command line
 */
abstract class ApolloDownloadSchemaCliTask : DefaultTask() {
  @get:Optional
  @get:Input
  @get:Option(option = "endpoint", description = "url of the GraphQL endpoint")
  abstract val endpoint: Property<String>

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

  @TaskAction
  fun taskAction() {
    val candidates = compilationUnits.filter {
      if (variant.isPresent && it.variantName == variant.get()) {
        return@filter true
      }

      it.variantName == "main" || it.variantName == "release"
    }

    val compilationUnit = if (service.isPresent) {
      candidates.firstOrNull { it.serviceName == service.get() }
    } else {
      check(candidates.size <= 1) {
        "please specify the --service"
      }
      candidates.firstOrNull()
    }

    check(compilationUnit != null) {
      val services = compilationUnits.map { it.serviceName }.distinct().sorted().joinToString("\n")
      val variants = compilationUnits.map { it.variantName }.distinct().sorted().joinToString("\n")
      "Cannot find compilation unit, check variant and service.\nPossible services:\n$services\nPossible variants:\n$variants"
    }

    val (compilerParams, _) = compilationUnit.resolveParams(project)

    val endpointProp = project.findProperty("com.apollographql.apollo.endpoint") as? String
    var endpointUrl = when {
      endpoint.isPresent -> endpoint.get()
      endpointProp != null -> {
        logger.lifecycle("Using the com.apollographql.apollo.endpoint property is deprecated. Use --endpoint instead.")
        endpointProp
      }
      else -> compilationUnit.service.introspection?.endpointUrl?.get()
    }
    check(endpointUrl != null) {
      "Specify the endpoint either with --endpoint or the introspection {} block"
    }

    val schemaProp = project.findProperty("com.apollographql.apollo.schema") as? String
    val schemaFile = when {
      schema.isPresent -> File(schema.get())
      schemaProp != null -> {
        logger.lifecycle("Using the com.apollographql.apollo.schema property is deprecated. Use --schema instead.")
        project.file(schemaProp)
      }
      else -> compilerParams.schemaFile.asFile.get()
    }

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
      endpointUrl = endpointUrl.toHttpUrl().newBuilder()
          .apply {
            ApolloPlugin.toMap(queryParamsProp).entries.forEach {
              addQueryParameter(it.key, it.value)
            }
          }
          .build()
          .toString()
    }

    SchemaDownloader.download(
        endpoint = endpointUrl,
        schema = schemaFile,
        headers = headers,
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
