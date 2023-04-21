package com.apollographql.ijplugin.project

import com.apollographql.ijplugin.codegen.ApolloCodegenService
import com.apollographql.ijplugin.gradle.GradleToolingModelService
import com.apollographql.ijplugin.graphql.GraphQLConfigService
import com.apollographql.ijplugin.settings.SettingsService
import com.apollographql.ijplugin.util.logd
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener

internal class ApolloProjectManagerListener : ProjectManagerListener {
  override fun projectOpened(project: Project) {
    logd()

    // Initialize all services on project open.
    // But wait for 'smart mode' to do it.
    DumbService.getInstance(project).runWhenSmart {
      logd("isApolloKotlin3Project=" + project.apolloProjectService.isApolloKotlin3Project)
      project.service<ApolloCodegenService>()
      project.service<GradleToolingModelService>()
      project.service<SettingsService>()
      project.service<GraphQLConfigService>()

      project.apolloProjectService.isInitialized = true
    }
  }
}
