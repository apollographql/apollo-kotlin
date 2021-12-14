package com.apollographql.apollo3.gradle.internal

import com.apollographql.apollo3.gradle.api.Introspection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property


abstract class DefaultIntrospection: Introspection {
  abstract override val endpointUrl: Property<String>
  abstract override val headers: MapProperty<String, String>
  abstract override val schemaFile: RegularFileProperty
}