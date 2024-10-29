package com.apollographql.ijplugin.graphql

import com.apollographql.ijplugin.gradle.ApolloKotlinService
import com.apollographql.ijplugin.gradle.GradleToolingModelService
import com.apollographql.ijplugin.project.apolloProjectService
import com.apollographql.ijplugin.settings.projectSettingsState
import com.apollographql.ijplugin.util.logd
import com.intellij.lang.jsgraphql.ide.config.GraphQLConfigContributor
import com.intellij.lang.jsgraphql.ide.config.loader.GraphQLConfigKeys
import com.intellij.lang.jsgraphql.ide.config.loader.GraphQLRawConfig
import com.intellij.lang.jsgraphql.ide.config.loader.GraphQLRawProjectConfig
import com.intellij.lang.jsgraphql.ide.config.loader.GraphQLRawSchemaPointer
import com.intellij.lang.jsgraphql.ide.config.model.GraphQLConfig
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir

class ApolloGraphQLConfigContributor : GraphQLConfigContributor {
  override fun contributeConfigs(project: Project): Collection<GraphQLConfig> {
    logd()
    val projectDir = project.guessProjectDir() ?: return emptyList()
    // This can be called early, don't initialize services right away. It's ok because it's called again later.
    if (!project.apolloProjectService.isInitialized) return emptyList()

    if (!project.projectSettingsState.contributeConfigurationToGraphqlPlugin) return emptyList()

    return listOf(
        GraphQLConfig(
            project = project,
            dir = projectDir,
            file = null,
            rawData = GraphQLRawConfig(
                projects = project.service<GradleToolingModelService>().apolloKotlinServices.map { apolloKotlinService ->
                  apolloKotlinService.id.toString() to apolloKotlinService.toGraphQLRawProjectConfig()
                }.toMap()
            )
        )
    )
  }

  private fun ApolloKotlinService.toGraphQLRawProjectConfig() = GraphQLRawProjectConfig(
      schema = schemaPaths.map { GraphQLRawSchemaPointer(it) },
      include = operationPaths.map { "$it/**/*.graphql" },
      extensions = mapOf(EXTENSION_APOLLO_KOTLIN_SERVICE_ID to this.id.toString()) +
          (endpointUrl?.let {
            mapOf(
                GraphQLConfigKeys.EXTENSION_ENDPOINTS to mapOf(
                    serviceName to buildMap {
                      put(GraphQLConfigKeys.EXTENSION_ENDPOINT_URL, endpointUrl)
                      if (endpointHeaders != null) {
                        put(GraphQLConfigKeys.HEADERS, endpointHeaders)
                      }
                    }
                )
            )
          } ?: emptyMap())
  )

  companion object {
    const val EXTENSION_APOLLO_KOTLIN_SERVICE_ID = "apolloKotlinServiceId"
  }
}
