package com.apollographql.apollo.gradle.internal

import com.apollographql.apollo.gradle.api.Registry
import com.apollographql.apollo.gradle.api.SchemaConnection
import org.gradle.api.Action
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property

abstract class DefaultRegistry: Registry {
  abstract override val key: Property<String>
  abstract override val graph: Property<String>
  abstract override val graphVariant: Property<String>
  abstract override val schemaFile: RegularFileProperty

  var schemaConnection: Action<SchemaConnection>? = null
  override fun schemaConnection(connection: Action<SchemaConnection>) {
    this.schemaConnection = connection
  }
}