package com.apollographql.apollo.gradle.api

import org.gradle.api.provider.Property

interface Registry {
  /**
   * The Apollo key
   *
   * See https://www.apollographql.com/docs/studio/api-keys/ for how to get an API key
   */
  val key: Property<String>

  /**
   * The graph you want to download the schema from
   */
  val graph: Property<String>

  /**
   * The variant you want to download the schema from
   */
  val graphVariant: Property<String>
}