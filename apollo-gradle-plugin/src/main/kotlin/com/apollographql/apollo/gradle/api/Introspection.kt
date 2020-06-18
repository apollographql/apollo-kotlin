package com.apollographql.apollo.gradle.api

import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property

/**
 * [Introspection] represents a GraphQL endpoint and its introspection query used to retrieve a schema.
 */
interface Introspection {
  /**
   * The HTTP endpoint url
   *
   * This parameter is mandatory
   */
  val endpointUrl: Property<String>

  /**
   * query parameters if any required to get the introspection response
   *
   * empty by default
   */
  @Deprecated("add query parameters to endpointUrl instead")
  val queryParameters: MapProperty<String, String>

  /**
   * HTTP headers if any required to get the introspection response
   *
   * empty by default
   */
  val headers: MapProperty<String, String>

  /**
   * The name of the sourceSet where to download the schema. By default it will be downloaded
   * in the "main" sourceSet (src/main/graphql)
   */
  val sourceSetName: Property<String>
}