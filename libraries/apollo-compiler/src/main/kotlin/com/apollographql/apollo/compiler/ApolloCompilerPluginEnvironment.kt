package com.apollographql.apollo.compiler

import com.apollographql.apollo.annotations.ApolloExperimental

/**
 * [ApolloCompilerPluginEnvironment] contains the environment where the Apollo compiler is run.
 */
@ApolloExperimental
class ApolloCompilerPluginEnvironment(
    /**
     * Arguments as passed from the Gradle plugin
     */
    val arguments: Map<String, Any?>,
    /**
     * A logger that can be used by the plugin.
     */
    val logger: ApolloCompilerPluginLogger,
)