package com.apollographql.apollo3.gradle.internal

import com.apollographql.apollo3.gradle.api.Service
import com.apollographql.apollo3.gradle.api.javaConventionOrThrow
import com.apollographql.apollo3.gradle.api.kotlinProjectExtensionOrThrow
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
    project.javaConventionOrThrow
        .sourceSets
        .getByName(name)
        .java
        .srcDir(outputDir)
  }

  override fun connectToAndroidVariant(variant: Any) {
    connectToAndroidVariant(project, variant, outputDir, task.get())
  }

  override fun connectToAndroidSourceSet(name: String) {
    connectToAndroidSourceSet(project, name, outputDir, task.get())
  }
}
