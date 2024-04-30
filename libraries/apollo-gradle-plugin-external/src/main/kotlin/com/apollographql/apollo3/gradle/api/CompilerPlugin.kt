package com.apollographql.apollo3.gradle.api

import com.apollographql.apollo3.annotations.ApolloExperimental

@ApolloExperimental
interface CompilerPlugin {
  /**
   * Adds the given argument to the [com.apollographql.apollo3.compiler.ApolloCompilerPlugin].
   * If two arguments are added with the same name, the second one overwrites the first one.
   *
   * @param name the name of the argument
   * @param value the value of the argument. One of:
   * - [String]
   * - [Int]
   * - [Double]
   * - [Boolean]
   * - [List]
   * - [Map]
   */
  fun argument(name: String, value: Any)
}