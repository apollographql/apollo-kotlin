package com.apollographql.ijplugin.util

import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile

val Project.isApolloAndroid2Project: Boolean
  get() = dependsOn("com.apollographql.apollo")

val Project.isApolloKotlin3Project: Boolean
  get() = dependsOn("com.apollographql.apollo3")

private fun Project.dependsOn(groupId: String): Boolean {
  var found = false
  service<ProjectRootManager>().orderEntries().librariesOnly().forEachLibrary { library ->
    logd("library=${library.name}")
    if (library.name?.contains("$groupId:") == true) {
      found = true
      false
    } else {
      true
    }
  }
  return found
}

fun Module.apolloGeneratedSourcesRoot(): VirtualFile? {
  return this.rootManager.contentRoots.first { it.path.contains("generated/source/apollo") }
}
