package com.apollographql.apollo.gradle.api

import com.apollographql.apollo.gradle.internal.RegisterOperations
import org.gradle.api.Action
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

/**
 * A [Service] represents a GraphQL endpoint and its associated schema.
 *
 * It inherits [Service] so individual parameters can be directly set on the [Service] and will be the
 * default for all [CompilationUnit] based on this service.
 */
interface Service : CompilerParams {
  /**
   * Configures the [Introspection]
   */
  fun introspection(configure: Action<in Introspection>)

  /**
   * Configures the [Introspection]
   */
  fun registerOperations(configure: Action<in RegisterOperationsConfig>)

  /**
   * path to the folder containing the graphql files relative to the current source set
   * (src/$foo/graphql/$sourceFolder). The plugin will compile all graphql files accross all source sets
   * in each variant.
   *
   * By default sourceFolder is ".", i.e it uses everything under src/$foo/graphql
   */
  val sourceFolder: Property<String>

  /**
   * path to the schema file relative to the current source set (src/$foo/graphql/$schemaPath). The plugin
   * will search all possible source sets in the variant.
   *
   * By default, the plugin looks for a "schema.json" file in the sourceFolders
   */
  val schemaPath: Property<String>

  /**
   * Files to exclude from the graphql files as in [org.gradle.api.tasks.util.PatternFilterable]
   */
  val exclude: ListProperty<String>
}
