package com.apollographql.apollo.gradle.internal

import com.apollographql.apollo.compiler.ApolloCompilerPlugin
import com.apollographql.apollo.compiler.ApolloCompilerPluginEnvironment
import com.apollographql.apollo.compiler.ApolloCompilerPluginProvider
import com.apollographql.apollo.compiler.internal.GradleCompilerPluginLogger
import java.util.ServiceLoader

internal fun apolloCompilerPlugin(
    arguments: Map<String, Any?>,
    logLevel: Int,
    warnIfNotFound: Boolean = false,
): ApolloCompilerPlugin? {
  val plugins = ServiceLoader.load(ApolloCompilerPlugin::class.java).toList()

  if (plugins.size > 1) {
    error("Apollo: only a single compiler plugin is allowed")
  }

  val plugin = plugins.singleOrNull()
  if (plugin != null) {
    error("Apollo: use ApolloCompilerPluginProvider instead of ApolloCompilerPlugin directly. ApolloCompilerPluginProvider allows arguments and logging")
  }

  val pluginProviders = ServiceLoader.load(ApolloCompilerPluginProvider::class.java).toList()

  if (pluginProviders.size > 1) {
    error("Apollo: only a single compiler plugin provider is allowed")
  }

  if (pluginProviders.isEmpty() && warnIfNotFound) {
    println("Apollo: a compiler plugin was added with `Service.plugin()` but could not be loaded by the ServiceLoader. Check your META-INF/services/com.apollographql.apollo.compiler.ApolloCompilerPluginProvider file.")
  }

  val provider = pluginProviders.singleOrNull()
  if (provider != null) {
    return provider.create(
        ApolloCompilerPluginEnvironment(
            arguments,
            GradleCompilerPluginLogger(logLevel)
        )
    )
  }

  return plugins.singleOrNull()
}

