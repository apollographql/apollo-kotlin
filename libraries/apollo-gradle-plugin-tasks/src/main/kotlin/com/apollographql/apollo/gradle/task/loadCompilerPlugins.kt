package com.apollographql.apollo.gradle.task

import com.apollographql.apollo.compiler.ApolloCompiler
import com.apollographql.apollo.compiler.ApolloCompilerPlugin
import com.apollographql.apollo.compiler.ApolloCompilerPluginEnvironment
import java.util.ServiceLoader

internal fun loadCompilerPlugins(
    arguments: Map<String, Any?>,
    logger: ApolloCompiler.Logger,
    classLoader: ClassLoader,
    warnIfNotFound: Boolean,
): List<ApolloCompilerPlugin> {
  val plugins = ServiceLoader.load(ApolloCompilerPlugin::class.java, classLoader).toMutableList()

  @Suppress("DEPRECATION")
  val pluginProviders = ServiceLoader.load(com.apollographql.apollo.compiler.ApolloCompilerPluginProvider::class.java, classLoader).toList()
  pluginProviders.forEach {
    // We make an exception for our own cache plugin because we want to display a nice error message to users before 4.3
    if (it.javaClass.name != "com.apollographql.cache.apollocompilerplugin.ApolloCacheCompilerPluginProvider") {
      println("Apollo: using ApolloCompilerPluginProvider is deprecated. You can use ApolloCompilerPlugin directly. See https://go.apollo.dev/ak-compiler-plugins for more details.")
    }
    plugins.add(it.create(ApolloCompilerPluginEnvironment(arguments, logger)))
  }

  if (plugins.isEmpty() && warnIfNotFound) {
    println("Apollo: a compiler plugin was added with `Service.plugin()` but no plugin was loaded by the ServiceLoader. Check your META-INF/services/com.apollographql.apollo.compiler.ApolloCompilerPlugin file. See https://go.apollo.dev/ak-compiler-plugins for more details.")
  }
  return plugins
}
