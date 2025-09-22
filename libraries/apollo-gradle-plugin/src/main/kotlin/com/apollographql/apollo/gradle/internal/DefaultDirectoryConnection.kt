package com.apollographql.apollo.gradle.internal

import com.apollographql.apollo.AgpCompat
import com.apollographql.apollo.ComponentFilter
import com.apollographql.apollo.gradle.api.Service
import com.apollographql.com.apollographql.apollo.Agp8
import com.apollographql.com.apollographql.apollo.Agp8Component
import com.apollographql.com.apollographql.apollo.Agp9
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider

internal class DefaultDirectoryConnection(
    private val project: Project,
    private val agp: AgpCompat?,
    override val task: TaskProvider<out Task>,
    override val outputDir: Provider<Directory>,
    /**
     * This is a workaround so that calling outputDir.get() in registerJavaGeneratingTask
     * doesn't eagerly create the tasks.
     * This is OK to do on Android because the taskProvider is also passed to setup task
     * dependencies.
     */
    private val hardCodedOutputDir: Provider<Directory>,
    /**
     * AGP changes the outputDirectory and needs this.
     * See https://issuetracker.google.com/u/1/issues/382215754
     */
    private val wiredWith: (Task) -> DirectoryProperty,
) : Service.DirectoryConnection {
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

  override fun connectToAndroidComponent(component: Any, kotlin: Boolean) {
    check(agp != null) {
      "Apollo: Android Gradle Plugin not found."
    }
    when (agp) {
      is Agp8 -> {
        agp.registerSourceGeneratingTask(component, hardCodedOutputDir, task)
      }

      is Agp9 -> {
        agp.addGeneratedSourceDirectory(component, task, wiredWith, kotlin)
      }
    }
  }

  override fun connectToAndroidVariants(kotlin: Boolean) {
    check(agp != null) {
      "Apollo: Android Gradle Plugin not found"
    }
    agp.onComponent(ComponentFilter.Main) {
      connectToAndroidComponent(it.wrappedComponent, kotlin)
    }
  }

  override fun connectToAndroidTestComponents(kotlin: Boolean) {
    check(agp != null) {
      "Apollo: Android Gradle Plugin not found"
    }
    agp.onComponent(ComponentFilter.Test) {
      connectToAndroidComponent(it.wrappedComponent, kotlin)
    }
  }

  @Deprecated("use connectToAndroidComponent() instead", replaceWith = ReplaceWith("connectToAndroidComponent(variant)"))
  override fun connectToAndroidVariant(variant: Any) {
    connectToAndroidComponent(variant, true)
  }

  @Deprecated("This function is deprecated and fails with AGP9+. Use connectToAndroidVariant instead")
  override fun connectToAndroidSourceSet(name: String) {
    check(agp != null) {
      "Apollo: Android Gradle Plugin not found"
    }
    agp.onComponent(ComponentFilter.Main) {
      if (it is Agp8Component) {
        if (it.sourceSets.contains(name)) {
          connectToAndroidComponent(it.wrappedComponent, true)
        }
      } else {
        error("connectToAndroidSourceSet is not supported with AGP9+")
      }
    }
  }

  @Deprecated("Use connectToAndroidVariants() instead")
  override fun connectToAllAndroidVariants() {
    // java seems to work for both Kotlin and Java
    connectToAndroidVariants(false)
  }
}
