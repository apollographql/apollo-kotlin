package com.apollographql.apollo.compiler

import com.apollographql.apollo.annotations.ApolloExperimental

/**
 * [ApolloCompilerPluginProvider] is entry point for creating [ApolloCompilerPlugin].
 *
 * [ApolloCompilerPluginProvider] is created by [java.util.ServiceLoader], make sure to include a matching `META-INF/services` resource.
 */
@ApolloExperimental
fun interface ApolloCompilerPluginProvider {
  /**
   * Creates the [ApolloCompilerPlugin]
   */
  fun create(environment: ApolloCompilerPluginEnvironment): ApolloCompilerPlugin
}

