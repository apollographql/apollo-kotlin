package com.apollographql.apollo.compiler

/**
 * [ApolloCompilerPluginEnvironment] contains the environment where the Apollo compiler is run.
 */
class ApolloCompilerPluginEnvironment(
    /**
     * @see [ApolloCompilerPluginValue]
     */
    val arguments: Map<String, ApolloCompilerPluginValue>,
    /**
     * A logger that can be used by the plugin.
     */
    val logger: ApolloCompilerPluginLogger,
)

/**
 * An argument value for the plugin.
 *
 * In a Gradle context, these values are used as task inputs as well as passed around classloader.
 *
 * Prefer using simple classes from the bootstrap classloader:
 * - [String]
 * - [Int]
 * - [Double]
 * - [Boolean]
 * - [List]
 * - [Map]
 */
typealias ApolloCompilerPluginValue = Any?