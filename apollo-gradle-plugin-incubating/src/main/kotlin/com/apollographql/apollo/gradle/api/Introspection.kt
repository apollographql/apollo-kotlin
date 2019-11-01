package com.apollographql.apollo.gradle.api

import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property

interface Introspection {
  val endpointUrl: Property<String>
  fun endpointUrl(endpointUrl: String)

  val queryParameters: MapProperty<String, String>
  fun queryParameters(queryParameters: Map<String, String>)

  val headers: MapProperty<String, String>
  fun headers(headers: Map<String, String>)

  val sourceSetName: Property<String>
  fun sourceSetName(sourceSetName: String)
}