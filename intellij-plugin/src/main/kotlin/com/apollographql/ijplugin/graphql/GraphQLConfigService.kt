package com.apollographql.ijplugin.graphql

import com.apollographql.ijplugin.util.logd
import com.intellij.lang.jsgraphql.ide.config.GraphQLConfigProvider
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

class GraphQLConfigService(
    private val project: Project,
) : Disposable {
  init {
    logd("project=${project.name}")
    project.messageBus.connect(this).subscribe(GraphQLProjectFilesListener.TOPIC, object : GraphQLProjectFilesListener {
      override fun projectFilesAvailable() {
        logd("Calling scheduleConfigurationReload")
        project.service<GraphQLConfigProvider>().scheduleConfigurationReload()
      }
    })
  }

  override fun dispose() {
    logd("project=${project.name}")
  }
}
