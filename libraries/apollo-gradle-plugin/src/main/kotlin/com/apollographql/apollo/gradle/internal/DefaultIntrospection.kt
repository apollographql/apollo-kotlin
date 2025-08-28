package com.apollographql.apollo.gradle.internal

import com.apollographql.apollo.gradle.api.Introspection
import com.apollographql.apollo.gradle.api.SchemaConnection
import org.gradle.api.Action
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property


abstract class DefaultIntrospection: Introspection {
  abstract override val endpointUrl: Property<String>
  abstract override val headers: MapProperty<String, String>
  abstract override val schemaFile: RegularFileProperty

  var schemaConnection: Action<SchemaConnection>? = null
  override fun schemaConnection(connection: Action<SchemaConnection>) {
    this.schemaConnection = connection
  }
}