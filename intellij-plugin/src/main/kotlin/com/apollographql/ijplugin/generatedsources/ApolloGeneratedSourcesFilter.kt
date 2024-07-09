package com.apollographql.ijplugin.generatedsources

import com.apollographql.ijplugin.ApolloBundle
import com.apollographql.ijplugin.util.isApolloGenerated
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.GeneratedSourcesFilter
import com.intellij.openapi.vfs.VirtualFile

class ApolloGeneratedSourcesFilter : GeneratedSourcesFilter() {
  override fun isGeneratedSource(file: VirtualFile, project: Project): Boolean {
    return file.isApolloGenerated()
  }

  override fun getNotificationText(file: VirtualFile, project: Project): String? {
    return ApolloBundle.message("generatedSourcesFilter.notificationText")
  }
}
