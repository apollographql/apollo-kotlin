package com.apollographql.apollo.gradle.api

import org.gradle.api.Action
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property

/**
 * A [Registry] represents a connection to the [Apollo Registry](https://www.apollographql.com/docs/studio/schema/registry/)
 *
 * Use this to register a `download${ServiceName}ApolloSchemaFromRegistry` task
 */
interface Registry {
  /**
   * The Apollo key
   *
   * See https://www.apollographql.com/docs/studio/api-keys/ for how to get an API key
   */
  val key: Property<String>

  /**
   * The graph you want to download the schema from
   */
  val graph: Property<String>

  /**
   * The variant you want to download the schema from
   */
  val graphVariant: Property<String>

  /**
   * The file where to download the schema.
   *
   * Uses the schema from the service by default
   */
  val schemaFile: RegularFileProperty

  /**
   * Use this to connect your schema to other parts of your build or just modify it
   */
  fun schemaConnection(connection: Action<SchemaConnection>)
}