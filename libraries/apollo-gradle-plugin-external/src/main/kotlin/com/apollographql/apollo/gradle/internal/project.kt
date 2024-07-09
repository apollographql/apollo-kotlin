package com.apollographql.apollo.gradle.internal

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

internal fun Project.mainSourceSet(): String {
  return when (project.extensions.findByName("kotlin")) {
    is KotlinMultiplatformExtension -> "commonMain"
    else -> "main"
  }
}
