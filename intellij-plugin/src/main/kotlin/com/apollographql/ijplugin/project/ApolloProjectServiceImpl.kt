package com.apollographql.ijplugin.project

import com.apollographql.ijplugin.util.isApolloAndroid2Project
import com.apollographql.ijplugin.util.isApolloKotlin3Project
import com.apollographql.ijplugin.util.logd
import com.intellij.ProjectTopics
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootListener

class ApolloProjectServiceImpl(
    private val project: Project,
) : ApolloProjectService, Disposable {

  override var isApolloAndroid2Project: Boolean = false
  override var isApolloKotlin3Project: Boolean = false
  override var isInitialized: Boolean = false

  init {
    logd("project=${project.name}")
    onLibrariesChanged()
    startObserveLibraries()
  }

  private fun startObserveLibraries() {
    logd()
    project.messageBus.connect(this).subscribe(ProjectTopics.PROJECT_ROOTS, object : ModuleRootListener {
      override fun rootsChanged(event: ModuleRootEvent) {
        logd("event=$event")
        onLibrariesChanged()
      }
    })
  }

  private fun onLibrariesChanged() {
    logd()
    synchronized(this) {
      isApolloAndroid2Project = project.isApolloAndroid2Project()
      isApolloKotlin3Project = project.isApolloKotlin3Project()
    }
    logd("isApolloAndroid2Project=$isApolloAndroid2Project isApolloKotlin3Project=$isApolloKotlin3Project")
    project.messageBus.syncPublisher(ApolloProjectListener.TOPIC).apolloProjectChanged(isApolloAndroid2Project = isApolloAndroid2Project, isApolloKotlin3Project = isApolloKotlin3Project)
  }

  override fun dispose() {
    logd("project=${project.name}")
  }
}
