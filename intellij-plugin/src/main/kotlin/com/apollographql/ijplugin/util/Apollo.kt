package com.apollographql.ijplugin.util

import com.apollographql.ijplugin.project.ApolloProjectService.ApolloVersion
import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile

const val apollo2 = "com.apollographql.apollo"
const val apollo3 = "com.apollographql.apollo3"
const val apollo4 = "com.apollographql.apollo"
const val apollo5 = "com.apollographql.apollo"

fun Project.getApolloVersion(): ApolloVersion {
  var foundVersion = ApolloVersion.NONE
  service<ProjectRootManager>().orderEntries().librariesOnly().forEachLibrary { library ->
    val mavenCoordinates = library.toMavenCoordinates() ?: return@forEachLibrary true
    @Suppress("DUPLICATE_LABEL_IN_WHEN")
    when (mavenCoordinates.group) {
      apollo2, apollo4, apollo5 -> {
        when {
          mavenCoordinates.version.startsWith("2.") -> {
            foundVersion = ApolloVersion.V2
            false
          }

          mavenCoordinates.version.startsWith("4.") -> {
            foundVersion = ApolloVersion.V4
            false
          }

          mavenCoordinates.version.startsWith("5.") -> {
            foundVersion = ApolloVersion.V5
            false
          }

          else -> true
        }
      }

      apollo3 -> {
        foundVersion = ApolloVersion.V3
        false
      }

      else -> true
    }
  }
  return foundVersion
}

fun Module.apolloGeneratedSourcesRoots(): List<VirtualFile> {
  return this.rootManager.contentRoots.filter { it.isApolloGenerated() }
}

fun VirtualFile.isApolloGenerated(): Boolean {
  return path.contains("generated/source/apollo")
}
