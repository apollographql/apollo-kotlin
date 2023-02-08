package com.apollographql.ijplugin.project

import com.apollographql.ijplugin.codegen.ApolloCodegenService
import com.apollographql.ijplugin.gradle.GradleToolingModelService
import com.apollographql.ijplugin.util.logd
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener
import org.jetbrains.kotlin.idea.util.application.getService

internal class ApolloProjectManagerListener : ProjectManagerListener {
  override fun projectOpened(project: Project) {
    logd()

    // Initialize all services on project open.
    // But wait for 'smart mode' to do it.
    DumbService.getInstance(project).runWhenSmart {
      logd("isApolloKotlin3Project=" + project.apolloProjectService.isApolloKotlin3Project)
      project.getService<ApolloCodegenService>()
      project.getService<GradleToolingModelService>()
    }
  }
}
