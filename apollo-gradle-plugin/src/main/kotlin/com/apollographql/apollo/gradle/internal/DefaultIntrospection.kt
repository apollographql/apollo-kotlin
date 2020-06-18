package com.apollographql.apollo.gradle.internal

import com.apollographql.apollo.gradle.api.Introspection
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import javax.inject.Inject


abstract class DefaultIntrospection @Inject constructor(val objects: ObjectFactory): Introspection {
  abstract override val endpointUrl: Property<String>

  abstract override val queryParameters: MapProperty<String, String>

  abstract override val headers: MapProperty<String, String>

  abstract override val sourceSetName: Property<String>
}