package com.apollographql.apollo.gradle.internal

import com.apollographql.apollo.gradle.api.ApolloExtension
import com.apollographql.apollo.gradle.internal.ApolloPluginHelper.Companion.isKotlinMultiplatform
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.util.GradleVersion
import java.net.URLDecoder

open class ApolloPlugin : Plugin<Project> {

  override fun apply(project: Project) {
    val pluginHelper = ApolloPluginHelper(project) {
      when {
        project.isKotlinMultiplatform -> true
        else -> it.generateKotlinModels.orElse(it.service.generateKotlinModels).orElse(it.apolloExtension.generateKotlinModels).getOrElse(false)
      }
    }

    project.afterEvaluate {
      // the extension block has not been evaluated yet, register a callback once the project has been evaluated
      // We need that because `generateKotlinSources` is needed to create the task graph
      pluginHelper.registerTasks(it)
    }
  }
}
