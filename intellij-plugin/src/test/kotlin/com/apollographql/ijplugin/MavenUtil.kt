package com.apollographql.ijplugin

import com.intellij.jarRepository.JarRepositoryManager
import com.intellij.jarRepository.RemoteRepositoryDescription
import com.intellij.jarRepository.RepositoryLibraryType
import com.intellij.openapi.roots.DependencyScope
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.impl.libraries.LibraryEx
import org.jetbrains.idea.maven.utils.library.RepositoryLibraryProperties

/**
 * This was copied from `com.intellij.testFramework.fixtures.MavenDependencyUtil#addFromMaven`, and modified to remove the call to
 * `getRemoteRepositoryDescriptions()` which only works when the `intellij-community` repo is available, and the `idea.home.path`
 * environment variable points to it.
 *
 * For this to work a "mock jdk" must still be present in `<idea.home.path>/java/mockJDK-1.7/jre/lib/rt.jar`.
 *
 * See https://jetbrains-platform.slack.com/archives/CPL5291JP/p1664105522154139 and https://youtrack.jetbrains.com/issue/IJSDK-321
 */
fun addFromMaven(
    model: ModifiableRootModel,
    mavenCoordinates: String,
    includeTransitiveDependencies: Boolean,
    dependencyScope: DependencyScope,
) {
  val remoteRepositoryDescriptions = listOf(
      RemoteRepositoryDescription.MAVEN_CENTRAL,
  )
  val libraryProperties = RepositoryLibraryProperties(mavenCoordinates, includeTransitiveDependencies)
  val roots = JarRepositoryManager.loadDependenciesModal(
      model.project,
      libraryProperties,
      false,
      false,
      null,
      remoteRepositoryDescriptions
  )
  val tableModel = model.moduleLibraryTable.modifiableModel
  val library = tableModel.createLibrary(mavenCoordinates, RepositoryLibraryType.REPOSITORY_LIBRARY_KIND)
  val libraryModel = library.modifiableModel
  check(!roots.isEmpty()) { String.format("No roots for '%s'", mavenCoordinates) }
  for (root in roots) {
    libraryModel.addRoot(root.file, root.type)
  }
  (libraryModel as LibraryEx.ModifiableModelEx).properties = libraryProperties
  val libraryOrderEntry = model.findLibraryOrderEntry(library)
      ?: throw IllegalStateException("Unable to find registered library $mavenCoordinates")
  libraryOrderEntry.scope = dependencyScope
  libraryModel.commit()
  tableModel.commit()
}
