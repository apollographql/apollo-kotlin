package com.apollographql.apollo3.cli

import com.apollographql.apollo3.tooling.SchemaUploader
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import java.io.File

internal class PublishSchemaCommand: CliktCommand() {
  private val graph by option(help = "The identifier of the Apollo graph used to download the schema.")
  private val key by option(help = "The Apollo API key. See https://www.apollographql.com/docs/studio/api-keys/ for more information on how to get your API key.").required()
  private val graphVariant by option(help = "The variant of the Apollo graph used to download the schema.").default("current")
  private val schema by option(help = "The path to the schema file to be updated").default("https://graphql.api.apollographql.com/api/graphql")

  override fun run() {
    SchemaUploader.uploadSchema(
        key = key,
        graph = graph,
        variant = graphVariant,
        sdl = File(schema).readText()
    )
  }
}