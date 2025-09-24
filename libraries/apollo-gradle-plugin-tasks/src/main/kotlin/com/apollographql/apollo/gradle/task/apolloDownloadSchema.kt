package com.apollographql.apollo.gradle.task

import com.apollographql.apollo.compiler.APOLLO_VERSION
import com.apollographql.apollo.tooling.SchemaDownloader
import gratatouille.tasks.GInputFile
import gratatouille.tasks.GInternal
import gratatouille.tasks.GLogger
import gratatouille.tasks.GTask
import java.io.File

/**
 * A task to download a schema either from introspection or from the registry.
 */
@GTask(pure = false)
internal fun apolloDownloadSchema(
    endpoint: String?,
    graph: String?,
    key: String?,
    graphVariant: String?,
    registryUrl: String?,
    @GInternal schema: GInputFile,
    insecure: Boolean?,
    headers: Map<String, String>,
    logger: GLogger,
) {
  val extraHeaders = mapOf(
      "apollographql-client-name" to "apollo-gradle-plugin",
      "apollographql-client-version" to APOLLO_VERSION
  )
  SchemaDownloader.download(
      endpoint = endpoint,
      graph = graph,
      graphVariant = graphVariant ?: "current",
      key = key,
      registryUrl = registryUrl ?: "https://api.apollographql.com/graphql",
      schema = schema,
      insecure = insecure ?: false,
      headers = headers.toMap() + extraHeaders,
  )

  logger.lifecycle("Apollo: schema downloaded to ${schema.absolutePath}")
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