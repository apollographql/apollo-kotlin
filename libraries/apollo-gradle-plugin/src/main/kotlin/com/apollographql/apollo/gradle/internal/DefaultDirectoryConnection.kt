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
    override val outputDir: Provider<Directory>,
    /**
     * This is a workaround so that calling outputDir.get() in registerJavaGeneratingTask
     * doesn't eagerly create the tasks.
     * This is OK to do on Android because the taskProvider is also passed to setup task
     * dependencies.
     */
    private val hardCodedOutputDir: Provider<Directory>
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
    connectToAndroidVariant(variant, hardCodedOutputDir, task)
  }

  override fun connectToAndroidSourceSet(name: String) {
    connectToAndroidSourceSet(project, name, hardCodedOutputDir, task)
  }

  override fun connectToAllAndroidVariants() {
    connectToAllAndroidVariants(project, hardCodedOutputDir, task)
  }
}
