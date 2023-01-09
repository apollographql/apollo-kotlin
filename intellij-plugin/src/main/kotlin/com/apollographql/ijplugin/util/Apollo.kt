package com.apollographql.ijplugin.util

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager

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
