@file:Suppress("DEPRECATION")

package com.apollographql.apollo.gradle

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.Component
import com.android.build.api.variant.HasDeviceTests
import com.android.build.api.variant.HasHostTests
import org.gradle.api.Task
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.TaskProvider
import java.io.File

@EmbeddedGradleSymbol
class Agp9(
    override val version: String,
    androidComponents: Any,
    android: Any?,
    kotlinExtension: Any?,
) : AgpCompat {

  // androidComponents is always present
  private val androidComponents = androidComponents as AndroidComponentsExtension<*, *, *>

  // android might not be present if using KMP
  private val android = android as CommonExtension?
  private val androidTarget = kotlinExtension?.androidTarget

  init {
    this.androidComponents.registerSourceType("graphql")
  }

  override fun compileSdk(): String? {
    return if (android != null) {
      android.compileSdk?.toString()
    } else if (androidTarget != null) {
      androidTarget.compileSdk?.toString()
    } else {
      null
    }
  }

  override fun targetSdk(): Int? {
    return if (android is ApplicationExtension) {
      android.defaultConfig.targetSdk
    } else {
      // We can't get the targetSdk for libraries
      null
    }
  }

  override fun minSdk(): Int? {
    return if (android != null) {
      android.defaultConfig.minSdk
    } else if (androidTarget != null) {
      androidTarget.minSdk
    } else {
      null
    }
  }

  override fun onComponent(filter: ComponentFilter, block: (AgpComponent) -> Unit) {
    val main = filter in setOf(ComponentFilter.All, ComponentFilter.Main)
    val test = filter in setOf(ComponentFilter.All, ComponentFilter.Test)

    androidComponents.apply {
      onVariants(selector().all()) {
        if (main) {
          block(Agp9Component(it))
        }
        if (test) {
          if (it is HasHostTests) {
            it.hostTests.values.forEach {
              block(Agp9Component(it))
            }
          }
          if (it is HasDeviceTests) {
            it.deviceTests.values.forEach {
              block(Agp9Component(it))
            }
          }
        }
      }
    }
  }

  fun <T: Task> addGeneratedSourceDirectory(
      component: Any,
      taskProvider: TaskProvider<T>,
      wiredWith: (T) -> DirectoryProperty,
      kotlin: Boolean,
  ) {
    check(component is Component) {
      "Apollo: an instance of com.android.build.api.variant.Component was expected (found '$component')"
    }

    val sources = if (kotlin) {
      component.sources.kotlin ?: error("No Kotlin sources found")
    } else {
      component.sources.java ?: error("No Java sources found")
    }

    // See https://issuetracker.google.com/issues/376709932#comment7
    sources.addGeneratedSourceDirectory(taskProvider, wiredWith)
  }
}

@EmbeddedGradleSymbol
class Agp9Component(private val base: Component) : AgpComponent {
  override val name: String
    get() = base.name

  val graphQLDirectories: List<File>
    get() {
      return base.sources.getByName("graphql").static.get().map { it.asFile }
    }

  override val wrappedComponent: Any
    get() = base
}

private val Any.androidTarget: KotlinMultiplatformAndroidLibraryTarget?
  get() {
    if (this !is ExtensionAware) {
      return null
    }

    return extensions.findByName("android") as KotlinMultiplatformAndroidLibraryTarget?
  }