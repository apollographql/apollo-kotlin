package com.apollographql.apollo.gradle.internal

import com.apollographql.apollo.gradle.api.Service
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider

internal class DefaultDirectoryConnection(
    private val project: Project,
    override val task: TaskProvider<out Task>,
    override val outputDir: Provider<Directory>
): Service.DirectoryConnection {
  override fun connectToKotlinSourceSet(name: String) {
    project.kotlinProjectExtensionOrThrow.sourceSets.getByName(name).kotlin.srcDir(outputDir)
  }

  override fun connectToJavaSourceSet(name: String) {
    project.javaExtensionOrThrow
        .sourceSets
        .getByName(name)
        .java
        .srcDir(outputDir)
  }

  override fun connectToAndroidVariant(variant: Any) {
    connectToAndroidVariant(variant, outputDir, task)
  }

  override fun connectToAndroidSourceSet(name: String) {
    connectToAndroidSourceSet(project, name, outputDir, task)
  }

  override fun connectToAllAndroidVariants() {
    connectToAllAndroidVariants(project, outputDir, task)
  }
}
