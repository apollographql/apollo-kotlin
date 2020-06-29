package com.apollographql.apollo.gradle.internal

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.UnknownConfigurationException

open class ApolloPluginJava : Plugin<Project> {
  override fun apply(target: Project) {
    val pluginHelper = ApolloPluginHelper(target) { false }
    pluginHelper.registerTasks(target)
  }
}
