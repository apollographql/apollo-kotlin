package com.apollographql.apollo.gradle.api

import org.gradle.api.Action
import org.gradle.api.Task
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider

/**
 * [Introspection] represents a GraphQL endpoint used to retrieve a schema.
 *
 * Use this to register a `download${ServiceName}ApolloSchemaFromIntrospection` task
 */
interface Introspection {
  /**
   * The HTTP endpoint url
   *
   * This parameter is mandatory
   */
  val endpointUrl: Property<String>

  /**
   * HTTP headers if any required to get the introspection response
   *
   * empty by default
   */
  val headers: MapProperty<String, String>

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

class SchemaConnection(
    /**
     * The task that produces schema
     */
    val task: TaskProvider<out Task>,

    /**
     * A provider for the schema file. This [Provider] carries task dependencies so you can use it as input to other tasks
     */
    val downloadedSchema: Provider<RegularFile>,
)
