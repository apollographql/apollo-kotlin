package com.apollographql.apollo.gradle.internal

import com.apollographql.apollo.compiler.APOLLO_VERSION
import com.apollographql.apollo.gradle.api.ApolloExtension
import com.apollographql.apollo.gradle.api.ApolloGradleToolingModel
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalDependency
import org.gradle.tooling.provider.model.ToolingModelBuilder
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry
import javax.inject.Inject

open class ApolloPlugin
@Inject
constructor(private val toolingModelRegistry: ToolingModelBuilderRegistry) : Plugin<Project> {
  override fun apply(project: Project) {
    val defaultService = project.objects.newInstance(DefaultService::class.java, project, "service")
    val apolloExtension: DefaultApolloExtension = project.extensions.create(ApolloExtension::class.java, "apollo", DefaultApolloExtension::class.java, project, defaultService) as DefaultApolloExtension

    project.configureDefaultVersionsResolutionStrategy()
    toolingModelRegistry.register(
        object : ToolingModelBuilder {
          override fun canBuild(modelName: String) = modelName == ApolloGradleToolingModel::class.java.name

          override fun buildAll(modelName: String, project: Project): ApolloGradleToolingModel {
            return DefaultApolloGradleToolingModel(
                projectName = project.name,
                projectPath = project.path,
                serviceInfos = apolloExtension.getServiceInfos(project),
                telemetryData = getTelemetryData(project, apolloExtension),
            )
          }
        }
    )
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
            .filter { it.group == "com.apollographql.apollo" && it.version.isNullOrEmpty() }
            .forEach { it.version { constraint -> constraint.require(pluginVersion) } }
      }
    }
  }


  companion object {
    internal val extraHeaders = mapOf(
        "apollographql-client-name" to "apollo-gradle-plugin",
        "apollographql-client-version" to APOLLO_VERSION
    )
  }
}
