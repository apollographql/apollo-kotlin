package com.apollographql.ijplugin.project

import com.apollographql.ijplugin.codegen.ApolloCodegenService
import com.apollographql.ijplugin.util.logd
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener
import org.jetbrains.kotlin.idea.util.application.getService

internal class ApolloProjectManagerListener : ProjectManagerListener {
  override fun projectOpened(project: Project) {
    logd("isApolloKotlin3Project=" + project.apolloProjectService.isApolloKotlin3Project)

    // Instantiate the codegen service to start monitoring the project
    project.getService<ApolloCodegenService>()
  }
}
