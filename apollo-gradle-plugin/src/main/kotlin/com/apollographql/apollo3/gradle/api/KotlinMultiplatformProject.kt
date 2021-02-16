package com.apollographql.apollo3.gradle.api

import com.apollographql.apollo3.gradle.internal.ApolloGenerateSourcesTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

object KotlinMultiplatformProject {
  fun registerGeneratedDirectoryToCommonMainSourceSet(project: Project, wire: Service.OutputDirWire) {
    val kotlinMultiplatformExtension = project.kotlinMultiplatformExtension
    check(kotlinMultiplatformExtension != null) {
      "ApolloGraphQL: no 'kotlin' extension found. Did you apply the Kotlin plugin?"
    }

    val sourceDirectorySet = kotlinMultiplatformExtension.sourceSets.getByName(KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME).kotlin

    // This should carry the task dependencies so nothing else should be required in theory
    sourceDirectorySet.srcDir(wire.outputDir)
  }
}
