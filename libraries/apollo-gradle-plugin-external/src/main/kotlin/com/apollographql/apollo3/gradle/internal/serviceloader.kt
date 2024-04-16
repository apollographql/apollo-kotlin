package com.apollographql.apollo3.gradle.internal

import com.apollographql.apollo3.compiler.ApolloCompilerPlugin
import java.util.ServiceLoader

internal fun apolloCompilerPlugin(warnIfNotFound: Boolean = false): ApolloCompilerPlugin? {
  val plugins = ServiceLoader.load(ApolloCompilerPlugin::class.java).toList()

  if (plugins.size > 1) {
    error("Apollo: only a single compiler plugin is allowed")
  }

  if (plugins.isEmpty() && warnIfNotFound) {
    println("Apollo: a compiler plugin was added with `Service.plugin()` but could not be loaded by the ServiceLoader. Check your META-INF.services/com.apollographql.apollo3.compiler.ApolloCompilerPlugin file.")
  }

  return plugins.singleOrNull()
}

