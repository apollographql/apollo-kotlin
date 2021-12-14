package com.apollographql.apollo3.gradle.internal

import com.apollographql.apollo3.gradle.api.Registry
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property

abstract class DefaultRegistry: Registry {
  abstract override val key: Property<String>
  abstract override val graph: Property<String>
  abstract override val graphVariant: Property<String>
  abstract override val schemaFile: RegularFileProperty
}