package com.apollographql.ijplugin.util

import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile

fun Project.isApolloAndroid2Project(): Boolean = dependsOn("com.apollographql.apollo")

fun Project.isApolloKotlin3Project(): Boolean = dependsOn("com.apollographql.apollo3")

private fun Project.dependsOn(groupId: String): Boolean {
  var found = false
  service<ProjectRootManager>().orderEntries().librariesOnly().forEachLibrary { library ->
    if (library.name?.contains("$groupId:") == true) {
      found = true
      false
    } else {
      true
    }
  }
  return found
}

fun Module.apolloGeneratedSourcesRoots(): List<VirtualFile> {
  return this.rootManager.contentRoots.filter { it.path.contains("generated/source/apollo") }
}
