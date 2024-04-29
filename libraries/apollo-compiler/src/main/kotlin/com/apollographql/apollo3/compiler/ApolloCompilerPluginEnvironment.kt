package com.apollographql.apollo3.compiler

import com.apollographql.apollo3.annotations.ApolloExperimental

@ApolloExperimental
class ApolloCompilerPluginEnvironment(
  val arguments: Map<String, Any?>,
  val logger: ApolloCompilerPluginLogger,
)