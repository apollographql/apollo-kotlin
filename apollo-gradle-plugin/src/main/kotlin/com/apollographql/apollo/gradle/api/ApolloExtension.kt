package com.apollographql.apollo.gradle.api

import com.apollographql.apollo.gradle.internal.DefaultService
import org.gradle.api.Action
import org.gradle.api.provider.Property

/**
 * The entry point for configuring the apollo plugin.
 *
 * The apollo plugin creates a ApolloGenerateSourcesTask for each [CompilationUnit]. A [CompilationUnit]
 * is the combination of:
 * - a [Service]: an entity that describes a particular schema and GraphQL endpoint. If your project
 * interacts with several endpoints, you will needed several [Service].
 * - a variant: for android, this is the different variants as in https://developer.android.com/studio/build/build-variants.
 * For JVM, this is only main right now.
 *
 * Most of the configuration relies on [CompilerParams]. The [CompilerParams] are resolved in this order:
 * - ApolloExtension
 * - Service
 * - CompilationUnit
 *
 * CompilationUnit values override Service values which override the global ApolloExtension values.
 */
interface ApolloExtension: CompilerParams {

  /**
   * registers a new service
   *
   * @param name: the name of the service, must be unique
   * @param action: the configure action for the [Service]
   */
  fun service(name: String, action: Action<DefaultService>)

  /**
   * [CompilationUnit] are created by the plugin based on the registered [Service]s and variants.
   * [onCompilationUnits] allows to retrieve and configure them.
   *
   * @param action: the configure action for the [CompilationUnit]
   */
  fun onCompilationUnits(action: Action<CompilationUnit>)

  /**
   * @Deprecated
   *
   * Used for backward compatibility reasons with the old groovy plugin
   */
  @Deprecated("please use services instead")
  val schemaFilePath: Property<String>

  /**
   * @Deprecated
   *
   * Used for backward compatibility reasons with the old groovy plugin
   */
  @Deprecated("please use services instead")
  fun setSchemaFilePath(schemaFilePath: String)

  /**
   * @Deprecated
   *
   * Used for backward compatibility reasons with the old groovy plugin
   */
  @Deprecated("please use services instead")
  val outputPackageName: Property<String>

  /**
   * @Deprecated
   *
   * Used for backward compatibility reasons with the old groovy plugin
   */
  @Deprecated("please use services instead")
  fun setOutputPackageName(outputPackageName: String)
}
