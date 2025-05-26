/**
 * Helpers for Kotlin projects
 *
 * They are in a separate file so that the corresponding class is not loaded
 * if the Kotlin plugin is not present
 */
package com.apollographql.apollo.gradle.internal

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension

internal val Project.isKotlinMultiplatform get() = pluginManager.hasPlugin("org.jetbrains.kotlin.multiplatform")

internal val Project.kotlinMultiplatformExtension
  get() = extensions.findByName("kotlin") as? KotlinMultiplatformExtension

internal val Project.kotlinProjectExtension
  get() = extensions.findByName("kotlin") as? KotlinProjectExtension

internal val Project.kotlinProjectExtensionOrThrow
  get() = kotlinProjectExtension
      ?: throw IllegalStateException("Apollo: no 'kotlin' extension found. Did you apply the Kotlin jvm plugin?")
