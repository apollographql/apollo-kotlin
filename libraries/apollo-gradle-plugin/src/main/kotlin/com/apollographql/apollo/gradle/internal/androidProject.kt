@file:Suppress("DEPRECATION")

package com.apollographql.apollo.gradle.internal

import com.android.build.gradle.AppExtension
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.TestedExtension
import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.api.LibraryVariant
import com.android.build.gradle.api.TestVariant
import com.android.build.gradle.api.UnitTestVariant
import com.android.builder.model.SourceProvider
import org.gradle.api.DomainObjectSet
import org.gradle.api.Project

internal class VariantWrapper(private val _wrapped: BaseVariant) {
  val name: String
    get() = _wrapped.name

  val sourceSets: List<SourceProvider>
    get() = _wrapped.sourceSets

  val wrapped: Any
    get() = _wrapped
}

internal object AndroidProject {
  fun onEachVariant(project: Project, withTestVariants: Boolean = false, block: (VariantWrapper) -> Unit) {
    project.applicationVariants?.configureEach {
      block(VariantWrapper(it))
    }
    project.libraryVariants?.configureEach {
      block(VariantWrapper(it))
    }

    if (withTestVariants) {
      project.testVariants?.configureEach {
        block(VariantWrapper(it))
      }
      project.unitTestVariants?.configureEach {
        block(VariantWrapper(it))
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