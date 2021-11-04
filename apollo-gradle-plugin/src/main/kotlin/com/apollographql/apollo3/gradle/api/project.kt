package com.apollographql.apollo3.gradle.api

import com.android.build.gradle.AppExtension
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.TestedExtension
import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.api.LibraryVariant
import com.android.build.gradle.api.TestVariant
import com.android.build.gradle.api.UnitTestVariant
import org.gradle.api.DomainObjectSet
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention

val Project.javaConventionOrThrow
  get() = convention.getPlugin(JavaPluginConvention::class.java)

val Project.javaConvention
  get() = try {
    javaConventionOrThrow
  } catch (e: Exception) {
    null
  }

val Project.androidExtension
  get() = extensions.findByName("android") as? BaseExtension

val Project.androidExtensionOrThrow
  get() = androidExtension ?: throw IllegalStateException("ApolloGraphQL: no 'android' extension found. Did you apply the Android plugin?")

val Project.isKotlinMultiplatform get() = pluginManager.hasPlugin("org.jetbrains.kotlin.multiplatform")

val Project.libraryVariants: DomainObjectSet<LibraryVariant>?
  get() {
    return (androidExtensionOrThrow as? LibraryExtension)
        ?.libraryVariants
  }

val Project.applicationVariants: DomainObjectSet<ApplicationVariant>?
  get() {
    return (androidExtensionOrThrow as? AppExtension)
        ?.applicationVariants
  }

val Project.unitTestVariants: DomainObjectSet<UnitTestVariant>?
  get() {
    return (androidExtensionOrThrow as? TestedExtension)
        ?.unitTestVariants
  }

val Project.testVariants: DomainObjectSet<TestVariant>?
  get() {
    return (androidExtensionOrThrow as? TestedExtension)
        ?.testVariants
  }
