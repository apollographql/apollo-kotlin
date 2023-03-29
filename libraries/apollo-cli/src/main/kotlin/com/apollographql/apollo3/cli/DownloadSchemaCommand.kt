package com.apollographql.apollo3.cli

import com.apollographql.apollo3.tooling.SchemaDownloader
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

internal class DownloadSchemaCommand: CliktCommand() {
  private val schema by option(help = "The path to the schema file to be updated").required()
  private val endpoint by option(help = "The url of the GraphQL endpoint for introspection")
  private val headers by option(help = "A JSON object representing the HTTP headers to use while introspecting `endpoint`")
  private val insecure: Boolean by option(help = "Do not verify certificates during download").flag()

  private val graph by option(help = "[Apollo Studio users only] The identifier of the Apollo graph used to download the schema.")
  private val key by option(help = "[Apollo Studio users only] The Apollo API key. See https://www.apollographql.com/docs/studio/api-keys/ for more information on how to get your API key.")
  private val graph_variant by option(help = "[Apollo Studio users only] The variant of the Apollo graph used to download the schema.").default("current")
  private val registryUrl by option(help = "[Apollo Studio users only] The registry url of the registry instance used to download the schema.").default("https://api.apollographql.com/graphql")

  override fun run() {
    val headersMap: Map<String, String> = headers?.let { headerJsonStr ->
      try {
        (Json.parseToJsonElement(headerJsonStr) as JsonObject).mapValues { it.value.jsonPrimitive.content }
      } catch (e: Exception) {
        error("'headers' must be a JSON object of the form {\"header1\": \"value1\", \"header2\": \"value2\"}")
      }
    } ?: emptyMap()

    SchemaDownloader.download(
        endpoint = endpoint,
        graph = graph,
        key = key,
        graphVariant = graph_variant,
        registryUrl = registryUrl,
        schema = File(schema),
        headers = headersMap,
        insecure = insecure,
    )
  }
}
