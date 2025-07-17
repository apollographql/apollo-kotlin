package com.apollographql.apollo.gradle.api

import com.apollographql.apollo.annotations.ApolloDeprecatedSince

@Deprecated("Use Service.pluginArgument() instead")
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v5_0_0)
interface CompilerPlugin {
  /**
   * Adds the given argument to the [com.apollographql.apollo.compiler.ApolloCompilerPlugin].
   * If two arguments are added with the same name, the second one overwrites the first one.
   *
   * Arguments are added as input to the codegen task.
   *
   * @param name the name of the argument
   * @param value the value for this argument
   *
   * @see com.apollographql.apollo.compiler.ApolloCompilerPluginValue
   */
  @Deprecated("Use Service.pluginArgument() instead")
  fun argument(name: String, value: Any?)
}