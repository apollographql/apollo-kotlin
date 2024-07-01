package com.apollographql.apollo.compiler

import com.apollographql.apollo.annotations.ApolloExperimental

/**
 * [ApolloCompilerPluginLogger] allows logging from the context of the Apollo compiler.
 *
 * Typically, the Apollo compiler is run from an isolated classloader and cannot use the Gradle logging functions but can respect the logging level set by the user.
 */
@ApolloExperimental
interface ApolloCompilerPluginLogger {
  fun logging(message: String)
  fun info(message: String)
  fun warn(message: String)
  fun error(message: String)
}
