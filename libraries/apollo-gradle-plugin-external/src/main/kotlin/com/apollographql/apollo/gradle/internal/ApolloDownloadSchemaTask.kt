package com.apollographql.apollo.gradle.internal

import com.apollographql.apollo.gradle.internal.ApolloPlugin.Companion.extraHeaders
import com.apollographql.apollo.tooling.SchemaDownloader
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import java.io.File

/**
 * A task to download a schema either from introspection or from the registry.
 *
 * This task can either be configured from the command line or from the gradle scripts
 */
abstract class ApolloDownloadSchemaTask : DefaultTask() {
  @get:Optional
  @get:Input
  @get:Option(option = "endpoint", description = "url of the GraphQL endpoint for introspection.")
  abstract val endpoint: Property<String>

  @get:Optional
  @get:Input
  @get:Option(option = "graph", description = "[Apollo Studio users only] The identifier of the Apollo graph used to download the schema.")
  abstract val graph: Property<String>

  @get:Optional
  @get:Input
  @get:Option(option = "key", description = "[Apollo Studio users only] The Apollo API key. See https://www.apollographql.com/docs/studio/api-keys/ for more information on how to get your API key.")
  abstract val key: Property<String>

  @get:Optional
  @get:Input
  @get:Option(option = "graphVariant", description = "[Apollo Studio users only] The variant of the Apollo graph used to download the schema.")
  abstract val graphVariant: Property<String>

  @get:Optional
  @get:Input
  @get:Option(option = "registryUrl", description = "[Apollo Studio users only] The registry url of the registry instance used to download the schema. Defaults to \"https://graphql.api.apollographql.com/api/graphql\"")
  abstract val registryUrl: Property<String>


  @get:Input
  @get:Optional
  @get:Option(option = "schema", description = "path where the schema will be downloaded, relative to the root project directory")
  abstract val schema: Property<String>

  /**
   * This is not declared as an output as it triggers this Gradle error else:
   * "Reason: Task ':root:generateServiceApolloCodegenSchema' uses this output of task ':root:downloadServiceApolloSchemaFromIntrospection' without declaring an explicit or implicit dependency."
   *
   * Since it's unlikely that users want to download the schema every time, just set it as an internal property.
   */
  @get:Internal
  abstract val outputFile: RegularFileProperty

  @get:Internal
  abstract var projectRootDir: String

  @get:Optional
  @get:Input
  @set:Option(option = "header", description = "HTTP headers in the form 'Name: Value'")
  var header = emptyList<String>() // cannot be abstract for @Option to work

  @get:Optional
  @get:Input
  @get:Option(option = "insecure", description = "if set to true, TLS/SSL certificates will not be checked when downloading")
  abstract val insecure: Property<Boolean>

  init {
    /**
     * We cannot know in advance if the backend schema changed so don't cache or mark this task up-to-date
     */
    outputs.upToDateWhen { false }
    outputs.cacheIf { false }
  }

  @TaskAction
  fun taskAction() {
    // Schema file is relative to the root project. It is not possible in a consistent way to have it relative to the current
    // working directory where the gradle command was started

    val file = if (outputFile.isPresent) {
      outputFile.asFile.get()
    } else {
      check(schema.isPresent) {
        "--schema is mandatory"
      }
      File(projectRootDir).resolve(schema.get())
    }
    SchemaDownloader.download(
        endpoint = endpoint.orNull,
        graph = graph.orNull,
        graphVariant = graphVariant.getOrElse("current"),
        key = key.orNull,
        registryUrl = registryUrl.getOrElse("https://api.apollographql.com/graphql"),
        schema = file,
        insecure = insecure.getOrElse(false),
        headers = header.toMap() + extraHeaders,
    )

    logger.info("Apollo: schema downloaded to ${file.absolutePath}")
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
