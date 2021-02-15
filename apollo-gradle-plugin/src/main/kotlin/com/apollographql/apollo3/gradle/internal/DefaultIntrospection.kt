package com.apollographql.apollo.gradle.internal

import com.apollographql.apollo.gradle.api.Introspection
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property


abstract class DefaultIntrospection: Introspection {
  abstract override val endpointUrl: Property<String>
  abstract override val headers: MapProperty<String, String>
}