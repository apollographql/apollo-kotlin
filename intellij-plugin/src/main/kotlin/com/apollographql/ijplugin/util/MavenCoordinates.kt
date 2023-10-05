package com.apollographql.ijplugin.util

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.roots.libraries.Library

data class MavenCoordinates(
    val group: String,
    val artifact: String,
    val version: String,
)

fun Library.toMavenCoordinates(): MavenCoordinates? {
  val components = name?.substringAfter(" ")?.split(":") ?: return null
  if (components.size < 3) return null
  val version = (if (components.size == 3) components[2] else components[3]).substringBeforeLast("@")
  return MavenCoordinates(components[0], components[1], version)
}

fun Project.getLibraryMavenCoordinates(): Set<MavenCoordinates> {
  val dependencies = mutableSetOf<MavenCoordinates>()
  service<ProjectRootManager>().orderEntries().librariesOnly().forEachLibrary { library ->
    library.toMavenCoordinates()?.let { dependencies.add(it) }
    true
  }
  return dependencies
}
