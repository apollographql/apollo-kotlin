package com.apollographql.apollo3.gradle.internal

import com.android.build.gradle.AppExtension
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.TestedExtension
import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.api.LibraryVariant
import com.android.build.gradle.api.TestVariant
import com.android.build.gradle.api.UnitTestVariant
import org.gradle.api.DomainObjectSet
import org.gradle.api.Project

internal object AndroidProject {
  fun onEachVariant(project: Project, withTestVariants: Boolean = false, block: (BaseVariant) -> Unit) {
    project.applicationVariants?.configureEach {
      block(it)
    }
    project.libraryVariants?.configureEach {
      block(it)
    }

    if (withTestVariants) {
      project.testVariants?.configureEach {
        block(it)
      }
      project.unitTestVariants?.configureEach {
        block(it)
      }
    }
  }
}

internal val Project.androidExtension
  get() = extensions.findByName("android") as? BaseExtension

internal val Project.androidExtensionOrThrow
  get() = androidExtension ?: throw IllegalStateException("Apollo: no 'android' extension found. Did you apply the Android plugin?")

internal val Project.libraryVariants: DomainObjectSet<LibraryVariant>?
  get() {
    return (androidExtensionOrThrow as? LibraryExtension)
        ?.libraryVariants
  }

internal val Project.applicationVariants: DomainObjectSet<ApplicationVariant>?
  get() {
    return (androidExtensionOrThrow as? AppExtension)
        ?.applicationVariants
  }

internal val Project.unitTestVariants: DomainObjectSet<UnitTestVariant>?
  get() {
    return (androidExtensionOrThrow as? TestedExtension)
        ?.unitTestVariants
  }

internal val Project.testVariants: DomainObjectSet<TestVariant>?
  get() {
    return (androidExtensionOrThrow as? TestedExtension)
        ?.testVariants
  }