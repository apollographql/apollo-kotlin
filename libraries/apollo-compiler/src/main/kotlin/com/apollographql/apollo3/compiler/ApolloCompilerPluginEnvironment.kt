package com.apollographql.apollo3.compiler

import com.apollographql.apollo3.annotations.ApolloExperimental

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
     * logger
     */
    val logger: ApolloCompilerPluginLogger,
)