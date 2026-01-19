@file:Suppress("DEPRECATION")

package com.apollographql.com.apollographql.apollo

import com.android.build.gradle.AppExtension
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.TestedExtension
import com.android.build.gradle.api.BaseVariant
import com.apollographql.apollo.AgpCompat
import com.apollographql.apollo.AgpComponent
import com.apollographql.apollo.ComponentFilter
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider

internal class Agp8(override val version: String, extension: Any): AgpCompat {
  private val extension = extension as BaseExtension

  override fun compileSdk(): String? {
    return extension.compileSdkVersion
  }

  override fun targetSdk(): Int? {
    return extension.defaultConfig.targetSdk
  }

  override fun minSdk(): Int? {
    return extension.defaultConfig.minSdkVersion?.apiLevel
  }

  override fun onComponent(filter: ComponentFilter, block: (AgpComponent) -> Unit) {
    val main = filter in setOf(ComponentFilter.All, ComponentFilter.Main)
    val test = filter in setOf(ComponentFilter.All, ComponentFilter.Test)

    if (main) {
      if (extension is LibraryExtension) {
        extension.libraryVariants.configureEach {
          block(Agp8Component(it))
        }
      }
      if (extension is AppExtension) {
        extension.applicationVariants.configureEach {
          block(Agp8Component(it))
        }
      }
    }
    if (test) {
      if (extension is TestedExtension) {
        extension.testVariants.configureEach {
          block(Agp8Component(it))
        }
        extension.unitTestVariants.configureEach {
          block(Agp8Component(it))
        }
      }
    }
  }

  fun registerSourceGeneratingTask(
      variant: Any,
      hardCodedOutputDir: Provider<Directory>,
      task: TaskProvider<*>,
  ) {
    check(variant is BaseVariant) {
      "Apollo: an instance of com.android.build.gradle.api.BaseVariant was expected (found '$variant')"
    }
    variant.registerJavaGeneratingTask(task, hardCodedOutputDir.get().asFile)
  }
}

internal class Agp8Component(private val base: BaseVariant): AgpComponent {
  override val name: String
    get() = base.name

  val sourceSets: List<String>
    get() = base.sourceSets.map { it.name }

  override val wrappedComponent: Any
    get() = base
}

