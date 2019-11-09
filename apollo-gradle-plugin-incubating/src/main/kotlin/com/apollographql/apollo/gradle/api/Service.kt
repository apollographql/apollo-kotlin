package com.apollographql.apollo.gradle.api

import org.gradle.api.Action
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

interface Service : CompilerParams {
  fun introspection(configure: Action<in Introspection>)

  val schemaPath: Property<String>
  fun schemaPath(schemaFilePath: String)

  val sourceFolder: Property<String>
  fun sourceFolder(sourceFolderPath: String)

  val exclude: ListProperty<String>
  fun exclude(exclude: List<String>)
}
