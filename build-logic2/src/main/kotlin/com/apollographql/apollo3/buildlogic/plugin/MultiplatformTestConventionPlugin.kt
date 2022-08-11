package com.apollographql.apollo3.buildlogic.plugin

import com.apollographql.apollo3.buildlogic.configureJavaAndKotlinCompilers
import com.apollographql.apollo3.buildlogic.configureMppTestsDefaults
import com.apollographql.apollo3.buildlogic.configureRepositories
import com.apollographql.apollo3.buildlogic.workaroundForIssueKT51970
import configureTesting
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class MultiplatformTestConventionPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    with(project) {
      configureRepositories()

      extensions.create("apolloConvention", Extension::class.java)

      pluginManager.apply {
        apply("org.jetbrains.kotlin.multiplatform")
        apply("com.apollographql.apollo3")
      }

      configureJavaAndKotlinCompilers()

      configureTesting()

      workaroundForIssueKT51970()
    }
  }

  abstract class Extension(private val project: Project) {
    fun kotlin(
        withJs: Boolean = true,
        withJvm: Boolean = true,
        newMemoryManager: Boolean = true,
        appleTargets: Set<String> = setOf("macosX64", "macosArm64"),
        configure: KotlinMultiplatformExtension.() -> Unit,
    ) {
      project.configureMppTestsDefaults(withJs = withJs, withJvm = withJvm, newMemoryManager = newMemoryManager, appleTargets = appleTargets)

      val kotlinExtension = project.extensions.findByName("kotlin") as KotlinMultiplatformExtension
      kotlinExtension.configure()
    }
  }
}
