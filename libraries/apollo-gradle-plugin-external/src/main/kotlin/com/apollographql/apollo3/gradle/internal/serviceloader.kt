package com.apollographql.apollo3.gradle.internal

import com.apollographql.apollo3.compiler.ApolloCompilerPlugin
import java.util.ServiceLoader

internal fun apolloCompilerPlugin(): ApolloCompilerPlugin? {
  var plugins = ServiceLoader.load(ApolloCompilerPlugin::class.java).toList()

  if (plugins.size > 1) {
    error("Apollo: only a single compiler plugin is allowed")
  }

  return plugins.singleOrNull()
}
