package com.apollographql.apollo.gradle.internal

import com.apollographql.apollo.gradle.api.Introspection
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import javax.inject.Inject


abstract class DefaultIntrospection @Inject constructor(val objects: ObjectFactory): Introspection {
  abstract override val endpointUrl: Property<String>
  override fun endpointUrl(endpointUrl: String) {
    this.endpointUrl.set(endpointUrl)
  }

  abstract override val queryParameters: MapProperty<String, String>
  override fun queryParameters(queryParameters: Map<String, String>) {
    this.queryParameters.set(queryParameters)
  }

  abstract override val headers: MapProperty<String, String>
  override fun headers(headers: Map<String, String>) {
    this.headers.set(headers)
  }

  abstract override val sourceSetName: Property<String>

  override fun sourceSetName(sourceSetName: String) {
    this.sourceSetName.set(sourceSetName)
  }

}