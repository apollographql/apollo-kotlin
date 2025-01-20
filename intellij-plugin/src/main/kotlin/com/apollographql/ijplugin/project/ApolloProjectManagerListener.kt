package com.apollographql.ijplugin.project

import com.apollographql.ijplugin.codegen.ApolloCodegenService
import com.apollographql.ijplugin.gradle.GradleToolingModelService
import com.apollographql.ijplugin.graphql.GraphQLConfigService
import com.apollographql.ijplugin.lsp.ApolloLspAppService
import com.apollographql.ijplugin.lsp.ApolloLspProjectService
import com.apollographql.ijplugin.settings.ProjectSettingsService
import com.apollographql.ijplugin.studio.fieldinsights.FieldInsightsService
import com.apollographql.ijplugin.studio.sandbox.SandboxService
import com.apollographql.ijplugin.telemetry.TelemetryService
import com.apollographql.ijplugin.util.isGradlePluginPresent
import com.apollographql.ijplugin.util.isKotlinPluginPresent
import com.apollographql.ijplugin.util.isLspAvailable
import com.apollographql.ijplugin.util.logd
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener
import com.intellij.util.application

internal class ApolloProjectManagerListener : ProjectManagerListener {
  override fun projectOpened(project: Project) {
    logd()

    // Initialize all services on project open.
    // But wait for 'smart mode' to do it.
    // Most of these services can't operate without the Kotlin and Gradle plugins (e.g. in RustRover).
    DumbService.getInstance(project).runWhenSmart {
      logd("apolloVersion=" + project.apolloProjectService.apolloVersion)
      if (isKotlinPluginPresent && isGradlePluginPresent) {
        project.service<ApolloCodegenService>()
        project.service<GraphQLConfigService>()
        project.service<GradleToolingModelService>()
        project.service<ProjectSettingsService>()
        project.service<SandboxService>()
        project.service<FieldInsightsService>()
        project.service<TelemetryService>()
      }
      if (isLspAvailable) {
        project.service<ApolloLspProjectService>()
        application.service<ApolloLspAppService>()
      }

      project.apolloProjectService.isInitialized = true
    }
  }
}
