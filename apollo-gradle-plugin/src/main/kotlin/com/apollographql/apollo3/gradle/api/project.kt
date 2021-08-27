package com.apollographql.apollo3.gradle.api

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
import org.gradle.api.internal.DefaultDomainObjectSet
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.plugins.JavaPluginExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension

val Project.kotlinMultiplatformExtension
  get() = extensions.findByName("kotlin") as? KotlinMultiplatformExtension

val Project.kotlinProjectExtension
  get() = extensions.findByName("kotlin") as? KotlinProjectExtension

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

val Project.kotlinProjectExtensionOrThrow
  get() = kotlinProjectExtension
      ?: throw IllegalStateException("ApolloGraphQL: no 'kotlin' extension found. Did you apply the Kotlin jvm plugin?")

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
