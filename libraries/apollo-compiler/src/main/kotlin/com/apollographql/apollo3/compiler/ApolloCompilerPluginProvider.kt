package com.apollographql.apollo3.compiler

import com.apollographql.apollo3.annotations.ApolloExperimental

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

