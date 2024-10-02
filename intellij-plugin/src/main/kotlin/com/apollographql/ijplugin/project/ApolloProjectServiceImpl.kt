package com.apollographql.ijplugin.project

import com.apollographql.ijplugin.project.ApolloProjectService.ApolloVersion
import com.apollographql.ijplugin.util.getApolloVersion
import com.apollographql.ijplugin.util.logd
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootListener

class ApolloProjectServiceImpl(
    private val project: Project,
) : ApolloProjectService, Disposable {

  override var apolloVersion: ApolloVersion = ApolloVersion.NONE
  override var isInitialized: Boolean = false

  init {
    logd("project=${project.name}")
    onLibrariesChanged()
    startObserveLibraries()
  }

  private fun startObserveLibraries() {
    logd()
    project.messageBus.connect(this).subscribe(ModuleRootListener.TOPIC, object : ModuleRootListener {
      override fun rootsChanged(event: ModuleRootEvent) {
        logd("event=$event")
        onLibrariesChanged()
      }
    })
  }

  private fun onLibrariesChanged() {
    logd()
    val previousApolloVersion = apolloVersion
    synchronized(this) {
      apolloVersion = project.getApolloVersion()
    }
    logd("apolloVersion=$apolloVersion")
    if (previousApolloVersion != apolloVersion) {
      project.messageBus.syncPublisher(ApolloProjectListener.TOPIC).apolloProjectChanged(apolloVersion = apolloVersion)
    }
  }

  override fun dispose() {
    logd("project=${project.name}")
  }
}
