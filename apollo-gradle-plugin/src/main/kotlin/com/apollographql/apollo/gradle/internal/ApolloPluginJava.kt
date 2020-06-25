package com.apollographql.apollo.gradle.internal

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.UnknownConfigurationException

open class ApolloPluginJava : Plugin<Project> {
  override fun apply(target: Project) {
    val pluginHelper = ApolloPluginHelper(target) { false }
    pluginHelper.registerTasks(target)

    target.afterEvaluate {
      if (pluginHelper.apolloExtension.addRuntimeDependency.orElse(true).get()) {
        val configuration = try {
          target.configurations.named("api")
        } catch (e: UnknownConfigurationException) {
          // If the java-library plugin is not applied, fallback to implementation
          target.configurations.named("implementation")
        }

        configuration.configure {
          it.dependencies.add(
              target.dependencies.create("com.apollographql.apollo:apollo-runtime:${com.apollographql.apollo.compiler.VERSION}")
          )
        }
      }
    }
  }
}
