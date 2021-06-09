package com.apollographql.apollo3.gradle.api

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property

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
}