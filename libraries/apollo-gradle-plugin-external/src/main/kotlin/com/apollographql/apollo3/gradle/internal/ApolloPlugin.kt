package com.apollographql.apollo3.gradle.internal

import com.apollographql.apollo3.compiler.APOLLO_VERSION
import com.apollographql.apollo3.gradle.api.ApolloExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalDependency

open class ApolloPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    val defaultService = project.objects.newInstance(DefaultService::class.java, project, "service")
    project.extensions.create(ApolloExtension::class.java, "apollo", DefaultApolloExtension::class.java, project, defaultService) as DefaultApolloExtension
    project.configureDefaultVersionsResolutionStrategy()
  }

  /**
   * Allow users to omit the Apollo version in their dependencies
   * Inspired by https://github.com/JetBrains/kotlin/blob/70e15b281cb43379068facb82b8e4bcb897a3c4f/libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/internal/KotlinDependenciesManagement.kt#L52
   */
  private fun Project.configureDefaultVersionsResolutionStrategy() {
    configurations.configureEach { configuration ->
      // Use the API introduced in Gradle 4.4 to modify the dependencies directly before they are resolved:
      configuration.withDependencies { dependencySet ->
        val pluginVersion = APOLLO_VERSION
        dependencySet.filterIsInstance<ExternalDependency>()
            .filter { it.group == "com.apollographql.apollo3" && it.version.isNullOrEmpty() }
            .forEach { it.version { constraint -> constraint.require(pluginVersion) } }
      }
    }
  }


  companion object {
    internal val extraHeaders = mapOf(
        "apollographql-client-name" to "apollo-gradle-plugin",
        "apollographql-client-version" to com.apollographql.apollo3.compiler.APOLLO_VERSION
    )
  }
}
