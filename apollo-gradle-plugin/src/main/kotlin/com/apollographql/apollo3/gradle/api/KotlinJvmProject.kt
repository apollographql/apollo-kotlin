package com.apollographql.apollo3.gradle.api

import com.android.build.gradle.api.BaseVariant
import com.apollographql.apollo3.gradle.api.Service
import com.apollographql.apollo3.gradle.api.kotlinJvmExtension
import com.apollographql.apollo3.gradle.api.kotlinMultiplatformExtension
import org.gradle.api.Project
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

object KotlinJvmProject {
  fun onEachSourceSet(project: Project, block: (KotlinSourceSet) -> Unit) {
    project.kotlinJvmExtensionOrFail.sourceSets.forEach {
      block(it)
    }
  }

  fun registerGeneratedDirectoryToMainSourceSet(project: Project, wire: Service.OutputDirWire) {
    project.kotlinJvmExtensionOrFail.sourceSets.getByName("main").kotlin.srcDir(wire.outputDir)
  }
}
