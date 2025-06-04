package com.apollographql.apollo.compiler

import com.apollographql.apollo.annotations.ApolloDeprecatedSince

/**
 * [ApolloCompilerPluginProvider] is entry point for creating [ApolloCompilerPlugin].
 *
 * [ApolloCompilerPluginProvider] is created by [java.util.ServiceLoader], make sure to include a matching `META-INF/services` resource.
 */
@Deprecated("Use ApolloCompilerPlugin directly.")
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_2_1)
fun interface ApolloCompilerPluginProvider {
  /**
   * Creates the [ApolloCompilerPlugin]
   */
  fun create(environment: ApolloCompilerPluginEnvironment): ApolloCompilerPlugin
}

