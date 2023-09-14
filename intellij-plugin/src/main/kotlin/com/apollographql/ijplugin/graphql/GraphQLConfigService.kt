package com.apollographql.ijplugin.graphql

import com.apollographql.ijplugin.gradle.ApolloKotlinServiceListener
import com.apollographql.ijplugin.util.logd
import com.intellij.lang.jsgraphql.ide.config.GraphQLConfigProvider
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

/**
 *  Listens to availability of Tooling model, and notifies the GraphQL plugin.
 */
@Service(Service.Level.PROJECT)
class GraphQLConfigService(
    private val project: Project,
) : Disposable {
  init {
    logd("project=${project.name}")
    project.messageBus.connect(this).subscribe(ApolloKotlinServiceListener.TOPIC, object : ApolloKotlinServiceListener {
      override fun apolloKotlinServicesAvailable() {
        logd("Calling scheduleConfigurationReload")
        project.service<GraphQLConfigProvider>().scheduleConfigurationReload()
      }
    })
  }

  override fun dispose() {
    logd("project=${project.name}")
  }
}
