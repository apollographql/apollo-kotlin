package com.apollographql.apollo3.gradle.api

import org.gradle.api.provider.Property

interface RegisterOperationsConfig {
  val listId: Property<String>

  val key: Property<String>

  val graph: Property<String>

  val graphVariant: Property<String>
}