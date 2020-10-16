package com.apollographql.apollo.gradle.internal

import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

object KotlinMultiplatformTaskConfigurator {
  fun registerGeneratedDirectory(kotlinMultiplatformExtension: KotlinMultiplatformExtension, codeGenProvider: TaskProvider<ApolloGenerateSourcesTask>) {
    val sourceDirectorySet = kotlinMultiplatformExtension.sourceSets.getByName(KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME).kotlin

    // This should carry the task dependencies so nothing else should be required in theory
    sourceDirectorySet.srcDir(codeGenProvider.flatMap { it.outputDir })
  }
}
